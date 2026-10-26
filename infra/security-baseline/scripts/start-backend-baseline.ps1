<#
.SYNOPSIS
    Arranca el jar del backend con el selector de seguridad elegido (baseline | legacy).

.DESCRIPTION
    Equivale a la configuración de ejecución de IntelliJ: un solo selector (APP_ADAPTERS_SECURITY_PROVIDER)
    gobierna seguridad e identidad. Carga en el proceso, sin imprimirlos:
      - el .env del backend (solo lectura),
      - en modo baseline, desde %USERPROFILE%\.asistencias-security-lab\lab.env: la credencial de la aplicación
        en el PEP y el secreto del cliente de aprovisionamiento.
    -DatasourceEnv carga DESPUÉS del .env un archivo con SPRING_DATASOURCE_URL/USERNAME/PASSWORD (p. ej. una DB de
    pruebas); fuera de Git. Se rechaza `sa`.

    Requiere el jar: .\mvnw.cmd -B -DskipTests package
#>
[CmdletBinding()]
param(
    [ValidateSet('baseline', 'legacy')]
    [string]$Provider = 'baseline',
    [int]$Port = 8080,
    [string]$BackendEnv,
    [string]$LabEnv,
    [string]$DatasourceEnv,
    [string]$PepUrl = 'http://localhost:18081',
    [string]$LogDirectory
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$scriptDirectory = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
$repoRoot = (Resolve-Path (Join-Path $scriptDirectory '..\..\..')).Path
if (-not $BackendEnv) { $BackendEnv = Join-Path $repoRoot '.env' }
if (-not $LabEnv) { $LabEnv = Join-Path $env:USERPROFILE '.asistencias-security-lab\lab.env' }
if (-not $LogDirectory) { $LogDirectory = Join-Path $env:TEMP 'asistencias-baseline' }
$jar = Get-ChildItem -Path (Join-Path $repoRoot 'target') -Filter '*.jar' |
    Where-Object { $_.Name -notlike '*plain*' } | Select-Object -First 1
if ($null -eq $jar) { throw 'No hay jar en target/. Ejecute .\mvnw.cmd -B -DskipTests package' }
if (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) { throw "El puerto $Port ya esta en uso." }

function Import-ProcessEnvFile([string]$Path) {
    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith('#')) { continue }
        $index = $trimmed.IndexOf('=')
        if ($index -lt 1) { continue }
        $values[$trimmed.Substring(0, $index).Trim()] = $trimmed.Substring($index + 1).Trim()
    }
    return $values
}

foreach ($pair in (Import-ProcessEnvFile $BackendEnv).GetEnumerator()) {
    [Environment]::SetEnvironmentVariable($pair.Key, $pair.Value, 'Process')
}
if ($DatasourceEnv) {
    foreach ($pair in (Import-ProcessEnvFile $DatasourceEnv).GetEnumerator()) {
        [Environment]::SetEnvironmentVariable($pair.Key, $pair.Value, 'Process')
    }
    if ($env:SPRING_DATASOURCE_USERNAME -eq 'sa') { throw 'DatasourceEnv usa sa; use un login de minimo privilegio.' }
}

$env:SERVER_PORT = "$Port"
$env:APP_ADAPTERS_SECURITY_PROVIDER = $Provider
Remove-Item Env:APP_ADAPTERS_IDENTITY_PROVIDER -ErrorAction SilentlyContinue  # un solo selector
if ($Provider -eq 'baseline') {
    $lab = Import-ProcessEnvFile $LabEnv
    foreach ($key in 'SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL_ASISTENCIAS', 'SECURITY_BASELINE_IDENTITY_ADMIN_CLIENT_SECRET') {
        if (-not $lab.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($lab[$key])) { throw "lab.env no define $key (ejecute provision_asistencias.py)." }
    }
    $env:SECURITY_BASELINE_PEP_URL = $PepUrl
    $env:SECURITY_BASELINE_PEP_APPLICATION_NAME = 'asistencias-uco'
    $env:SECURITY_BASELINE_PEP_ENVIRONMENT = 'dev'
    $env:SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL = $lab['SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL_ASISTENCIAS']
    $env:SECURITY_BASELINE_IDENTITY_ADMIN_CLIENT_SECRET = $lab['SECURITY_BASELINE_IDENTITY_ADMIN_CLIENT_SECRET']
}
New-Item -ItemType Directory -Force -Path $LogDirectory | Out-Null
$env:ASISTENCIAS_UCO_LOG_FILE = Join-Path $LogDirectory "backend-$Provider.jsonl"
$env:ASISTENCIAS_UCO_LOG_FILE_PATTERN = Join-Path $LogDirectory "backend-$Provider.%d{yyyy-MM-dd}.%i.jsonl"

Write-Host "Backend $Provider en http://127.0.0.1:$Port (logs: $LogDirectory)"
# La JVM se supervisa por su exit code, no por stderr (PowerShell 5.1 convierte stderr nativo en ErrorRecord).
$ErrorActionPreference = 'Continue'
& (Join-Path $env:JAVA_HOME 'bin\java.exe') -jar $jar.FullName
exit $LASTEXITCODE
