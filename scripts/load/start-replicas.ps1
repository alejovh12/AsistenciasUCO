<#
.SYNOPSIS
    VAL-oleada3 (REA-06 / F12-08): arranca 1 o 2 réplicas del backend en el host, contra la MISMA base de datos, para
    las pruebas de carga y de dos réplicas. -Stop las detiene.

.DESCRIPTION
    Reutiliza el launcher oficial infra\security-baseline\scripts\start-backend-baseline.ps1 (carga .env y lab.env en el
    proceso sin imprimirlos). Cada réplica i (0..N-1) usa:
      - API          : BasePort + 2*i            (8080, 8082)
      - gestión      : ManagementBasePort + 2*i  (8090, 8092)  -> /actuator/health/readiness y /actuator/prometheus
      - logs JSONL   : infra\observability\logs\replica-<i> (fuera de Git)
    El puerto de gestión y el heap se fijan con JAVA_TOOL_OPTIONS (propiedades de sistema, que en Spring ganan a las
    variables del .env) sin modificar el launcher. -ManagementAddress 0.0.0.0 (por defecto en este script) permite que
    el Traefik de pruebas (contenedor) haga health check por host.docker.internal; es SOLO para desarrollo local
    (docs/observability/README.md) y nunca debe exponerse fuera de la máquina.

    Estado (PID de cada réplica) en %TEMP%\asistencias-uco-replicas.json; -Stop mata el árbol de procesos registrado.

.EXAMPLE
    .\scripts\load\start-replicas.ps1 -Replicas 2
    .\scripts\load\start-replicas.ps1 -Stop
#>
[CmdletBinding()]
param(
    [ValidateRange(1, 4)][int]$Replicas = 1,
    [int]$BasePort = 8080,
    [int]$ManagementBasePort = 8090,
    [ValidateRange(256, 4096)][int]$HeapMb = 512,
    # Tamaño del pool Hikari por réplica (spring.datasource.hikari.maximum-pool-size). Ver docs/work-items/VAL-oleada3-carga.
    [ValidateRange(2, 200)][int]$PoolSize = 10,
    [string]$ManagementAddress = '0.0.0.0',
    [ValidateRange(30, 600)][int]$WaitSeconds = 240,
    [switch]$Stop
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$launcher = Join-Path $repoRoot 'infra\security-baseline\scripts\start-backend-baseline.ps1'
$statePath = Join-Path ([System.IO.Path]::GetTempPath()) 'asistencias-uco-replicas.json'

function Stop-Replicas {
    if (-not (Test-Path -LiteralPath $statePath)) { Write-Output 'STOP|replicas|NONE_REGISTERED'; return }
    $state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
    $all = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue)
    $tracked = New-Object 'System.Collections.Generic.HashSet[int]'
    foreach ($item in @($state)) { $null = $tracked.Add([int]$item.Pid) }
    $changed = $true
    while ($changed) {
        $changed = $false
        foreach ($p in $all) {
            if ($tracked.Contains([int]$p.ParentProcessId) -and -not $tracked.Contains([int]$p.ProcessId)) {
                $null = $tracked.Add([int]$p.ProcessId); $changed = $true
            }
        }
    }
    foreach ($trackedPid in @($tracked | Sort-Object -Descending)) { Stop-Process -Id $trackedPid -Force -ErrorAction SilentlyContinue }
    Remove-Item -LiteralPath $statePath -Force -ErrorAction SilentlyContinue
    Write-Output ('STOP|replicas|STOPPED_PROCESSES=' + $tracked.Count)
}

if ($Stop) { Stop-Replicas; exit 0 }
if (Test-Path -LiteralPath $statePath) { throw "Ya hay réplicas registradas en $statePath; ejecute -Stop primero." }

if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    # El launcher usa $env:JAVA_HOME\bin\java.exe; si no está definida se toma el java del PATH.
    # PowerShell 5.1 convierte el stderr nativo en ErrorRecord: se lee con 'Continue' para no abortar.
    $ErrorActionPreference = 'Continue'
    $javaHomeLine = (& java -XshowSettings:properties -version 2>&1 | ForEach-Object { "$_" } | Where-Object { $_ -match '^\s*java\.home\s*=' } | Select-Object -First 1)
    $ErrorActionPreference = 'Stop'
    if (-not $javaHomeLine) { throw 'JAVA_HOME no está definida y no se encontró java en el PATH.' }
    $env:JAVA_HOME = ($javaHomeLine -split '=', 2)[1].Trim()
}

$started = @()
try {
    for ($i = 0; $i -lt $Replicas; $i++) {
        $port = $BasePort + 2 * $i
        $mgmt = $ManagementBasePort + 2 * $i
        $logDir = Join-Path $repoRoot ("infra\observability\logs\replica-$i")
        $env:JAVA_TOOL_OPTIONS = "-Xmx${HeapMb}m -Dmanagement.server.port=$mgmt -Dmanagement.server.address=$ManagementAddress " +
            "-Dspring.datasource.hikari.maximum-pool-size=$PoolSize"
        $out = Join-Path ([System.IO.Path]::GetTempPath()) "asistencias-uco-replica-$i.out.log"
        $err = Join-Path ([System.IO.Path]::GetTempPath()) "asistencias-uco-replica-$i.err.log"
        $process = Start-Process -FilePath 'powershell.exe' -WindowStyle Hidden -PassThru -WorkingDirectory $repoRoot `
            -RedirectStandardOutput $out -RedirectStandardError $err `
            -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $launcher, '-Provider', 'baseline', '-Port', "$port", '-LogDirectory', $logDir)
        $started += [pscustomobject]@{ Replica = $i; Pid = $process.Id; Port = $port; ManagementPort = $mgmt; Out = $out }
        $started | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath $statePath -Encoding UTF8
        Write-Output ("START|replica-$i|PID=$($process.Id) API=$port MGMT=$mgmt")
    }
} finally {
    Remove-Item Env:JAVA_TOOL_OPTIONS -ErrorAction SilentlyContinue
}

$deadline = (Get-Date).AddSeconds($WaitSeconds)
foreach ($item in $started) {
    $ready = $false
    while (-not $ready -and (Get-Date) -lt $deadline) {
        try {
            $r = Invoke-WebRequest -UseBasicParsing -TimeoutSec 3 -Uri ("http://127.0.0.1:{0}/actuator/health/readiness" -f $item.ManagementPort)
            $ready = ($r.StatusCode -eq 200)
        } catch { Start-Sleep -Seconds 3 }
    }
    Write-Output ("READY|replica-{0}|{1}" -f $item.Replica, $(if ($ready) { 'UP' } else { 'TIMEOUT (ver ' + $item.Out + ')' }))
    if (-not $ready) { exit 1 }
}
exit 0
