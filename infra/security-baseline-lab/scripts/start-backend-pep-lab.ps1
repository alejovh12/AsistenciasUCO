<#
.SYNOPSIS
    Arranca el backend experimental (worktree SEC-BASELINE-001) en otro puerto con el PEP activo.

.DESCRIPTION
    No toca el backend estable (8080). Carga en el proceso, sin imprimirlos:
      - el .env del backend (solo lectura; por defecto el del checkout estable),
      - SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL desde lab.env.
    El flag queda activo solo en este proceso; application.yml conserva OFF por defecto.
    Requiere haber empaquetado antes: .\mvnw.cmd -B -ntp -DskipTests package
#>
[CmdletBinding()]
param(
    [int]$Port = 8083,
    [string]$BackendEnv = (Join-Path $env:USERPROFILE 'AsistenciasUCO\AsistenciasUCO\.env'),
    [string]$PepUrl = 'http://127.0.0.1:18081',
    [string]$ApplicationName = 'asistencias-uco-lab',
    [string]$LogDirectory = (Join-Path $env:TEMP 'asistencias-security-lab')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'LabCommon.psm1') -Force

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$jar = Get-ChildItem -Path (Join-Path $repoRoot 'target') -Filter '*.jar' |
    Where-Object { $_.Name -notlike '*plain*' } | Select-Object -First 1
if ($null -eq $jar) { throw 'No hay jar en target/. Ejecute .\mvnw.cmd -B -ntp -DskipTests package' }

if (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) {
    throw "El puerto $Port ya esta en uso; elija otro con -Port."
}

foreach ($line in Get-Content -LiteralPath $BackendEnv) {
    $trimmed = $line.Trim()
    if ($trimmed.Length -eq 0 -or $trimmed.StartsWith('#')) { continue }
    $index = $trimmed.IndexOf('=')
    if ($index -lt 1) { continue }
    [Environment]::SetEnvironmentVariable($trimmed.Substring(0, $index).Trim(), $trimmed.Substring($index + 1).Trim(), 'Process')
}

$lab = Import-LabEnv
if ([string]::IsNullOrWhiteSpace($lab['SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL'])) {
    throw 'lab.env no contiene SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL (registre la aplicacion en el PDP).'
}
New-Item -ItemType Directory -Force -Path $LogDirectory | Out-Null
$env:SERVER_PORT = "$Port"
$env:SECURITY_BASELINE_PEP_ENABLED = 'true'
$env:SECURITY_BASELINE_PEP_URL = $PepUrl
$env:SECURITY_BASELINE_PEP_APPLICATION_NAME = $ApplicationName
$env:SECURITY_BASELINE_PEP_ENVIRONMENT = 'local'
$env:SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL = $lab['SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL']
$env:ASISTENCIAS_UCO_LOG_FILE = Join-Path $LogDirectory 'backend-pep-lab.jsonl'
$env:ASISTENCIAS_UCO_LOG_FILE_PATTERN = Join-Path $LogDirectory 'backend-pep-lab.%d{yyyy-MM-dd}.%i.jsonl'

Write-Host "Backend experimental: http://127.0.0.1:$Port (PEP $PepUrl, app $ApplicationName). Logs: $LogDirectory"
& (Join-Path $env:JAVA_HOME 'bin\java.exe') -jar $jar.FullName
