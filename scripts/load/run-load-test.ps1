<#
.SYNOPSIS
    VAL-oleada3 (REA-01 / REA-06): corre la prueba de carga k6 (imagen oficial grafana/k6 fijada por digest) contra el
    backend local o contra el balanceador de dos réplicas, y deja la evidencia en docs/work-items/VAL-oleada3-carga/evidencias.

.DESCRIPTION
    1. Obtiene tokens de los usuarios de prueba con scripts/load/get_tokens.py (PKCE; nunca imprime contraseñas ni tokens)
       en un directorio temporal propio que se borra al final.
    2. Con -ProxyContainer (Traefik de docker-compose.replicas.yml del repo Infra) cuenta en su access log JSON las
       peticiones servidas por cada réplica (ServiceURL) y sus códigos, para demostrar el reparto entre réplicas.
       (El contador de Actuator no sirve: /actuator/prometheus del puerto de gestión responde 401, ver VALIDATION.md.)
    3. Ejecuta k6 en Docker (host.docker.internal llega al backend del host) y guarda el resumen JSON y el texto.

.EXAMPLE
    .\scripts\load\run-load-test.ps1 -Label una-replica
    .\scripts\load\run-load-test.ps1 -Label dos-replicas -BaseUrl http://host.docker.internal:8088 -ProxyContainer proxy_replicas_asistencias
    .\scripts\load\run-load-test.ps1 -Label pep-caido -Mode dependencia-caida -Duration 30s -Vus 10
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidatePattern('^[a-z0-9-]+$')][string]$Label,
    [string]$BaseUrl = 'http://host.docker.internal:8080',
    [string]$ProxyContainer,
    # REA-06: contenedor de una dependencia (p. ej. asistencias-security-lab-pep-1) que se detiene DESPUÉS de obtener los
    # tokens y ANTES de k6, y se vuelve a arrancar al terminar (aunque k6 falle).
    [string]$StopContainerDuringRun,
    [ValidateRange(1, 200)][int]$Vus = 20,
    [string]$Duration = '2m',
    [string]$Ramp = '20s',
    [int]$ThinkMs = 1000,
    [ValidateSet('normal', 'dependencia-caida')][string]$Mode = 'normal',
    [string]$EvidenceDir,
    # Imagen oficial de k6 fijada por digest (grafana/k6:1.3.0).
    [string]$K6Image = 'grafana/k6:1.3.0@sha256:3ddc8b1a33a2c3d8edc6e99b6a762ae36cba08788463458f5e6a7703e14eb77d'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
if (-not $EvidenceDir) { $EvidenceDir = Join-Path $repoRoot 'docs\work-items\VAL-oleada3-carga\evidencias' }
New-Item -ItemType Directory -Force -Path $EvidenceDir | Out-Null
$stamp = (Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssZ')
$work = Join-Path ([System.IO.Path]::GetTempPath()) ("uco-k6-" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path (Join-Path $work 'out') | Out-Null

function Get-ReplicaDistribution([string]$Container, [string]$Since) {
    $ErrorActionPreference = 'Continue'
    $lines = & docker logs --since $Since $Container 2>&1 | ForEach-Object { "$_" }
    $ErrorActionPreference = 'Stop'
    $groups = @{}
    foreach ($line in $lines) {
        if ($line -notmatch '"ServiceURL"') { continue }
        try { $entry = $line | ConvertFrom-Json } catch { continue }
        $key = "{0} status={1}" -f $entry.ServiceURL, $entry.DownstreamStatus
        if ($groups.ContainsKey($key)) { $groups[$key]++ } else { $groups[$key] = 1 }
    }
    return @($groups.GetEnumerator() | Sort-Object Name | ForEach-Object { "reparto $($_.Name) peticiones=$($_.Value)" })
}

try {
    $roles = 'ESTUDIANTE,DOCENTE,COORDINADOR'
    $tokens = Join-Path $work 'tokens.json'
    $ErrorActionPreference = 'Continue'
    & py -3 -X utf8 -I (Join-Path $PSScriptRoot 'get_tokens.py') --out $tokens --roles $roles
    $tokenExit = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($tokenExit -ne 0 -or -not (Test-Path -LiteralPath $tokens)) { throw 'No se pudieron obtener los tokens de prueba (¿lab de seguridad arriba?).' }

    $stopped = $false
    if ($StopContainerDuringRun) {
        & docker stop $StopContainerDuringRun | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "No se pudo detener $StopContainerDuringRun." }
        $stopped = $true
        Write-Output "DEPENDENCIA|$StopContainerDuringRun|DETENIDA"
    }
    $since = (Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ')

    $k6Dir = Join-Path $PSScriptRoot 'k6'
    $dockerArgs = @('run', '--rm', '--add-host', 'host.docker.internal:host-gateway',
        '-v', "${k6Dir}:/scripts:ro", '-v', "${work}:/work",
        '-e', "BASE_URL=$BaseUrl", '-e', "VUS=$Vus", '-e', "DURATION=$Duration", '-e', "RAMP=$Ramp",
        '-e', "THINK_MS=$ThinkMs", '-e', "MODE=$Mode", '-e', 'TOKENS_FILE=/work/tokens.json', '-e', 'SUMMARY_FILE=/work/out/summary.json',
        $K6Image, 'run', '--quiet', '/scripts/carga-lectura.js')
    $ErrorActionPreference = 'Continue'
    $k6Output = & docker @dockerArgs 2>&1 | ForEach-Object { "$_" }
    $k6Exit = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($stopped) {
        & docker start $StopContainerDuringRun | Out-Null
        $stopped = $false
        Write-Output "DEPENDENCIA|$StopContainerDuringRun|ARRANCADA"
    }

    $reparto = if ($ProxyContainer) { Get-ReplicaDistribution $ProxyContainer $since } else { @('reparto=n/a (sin balanceador)') }

    $base = Join-Path $EvidenceDir ("$stamp-$Label")
    $summary = Join-Path $work 'out\summary.json'
    if (Test-Path -LiteralPath $summary) { Copy-Item -LiteralPath $summary -Destination "$base.summary.json" }
    $header = @(
        "# VAL-oleada3 carga: $Label",
        "fecha_utc=$stamp",
        "k6_image=$K6Image",
        "base_url=$BaseUrl modo=$Mode vus=$Vus duracion=$Duration rampa=$Ramp think_ms=$ThinkMs",
        "dependencia_detenida=$(if ($StopContainerDuringRun) { $StopContainerDuringRun } else { 'ninguna' })",
        "k6_exit_code=$k6Exit (0 = todos los umbrales cumplen; 99 = algun umbral falla)"
    ) + $reparto + @('', '## salida k6') + $k6Output
    $header | Set-Content -LiteralPath "$base.txt" -Encoding UTF8
    $header | Write-Output
    Write-Output "EVIDENCIA|$base.txt"
} finally {
    if ($StopContainerDuringRun -and (Get-Variable stopped -ErrorAction SilentlyContinue) -and $stopped) { & docker start $StopContainerDuringRun | Out-Null }
    Remove-Item -LiteralPath $work -Recurse -Force -ErrorAction SilentlyContinue
}
