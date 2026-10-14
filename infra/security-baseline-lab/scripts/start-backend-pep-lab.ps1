<#
.SYNOPSIS
    Arranca el backend experimental (worktree SEC-BASELINE-001) en otro puerto con el PEP activo.

.DESCRIPTION
    No toca el backend estable (8080). Carga en el proceso, sin imprimirlos:
      - el .env del backend (solo lectura; por defecto el del checkout estable),
      - SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL desde lab.env.
    El flag queda activo solo en este proceso; application.yml conserva OFF por defecto.
    Requiere haber empaquetado antes: .\mvnw.cmd -B -ntp -DskipTests package

    -IdentityMode SharedLab (SEC-BASELINE-002) sustituye, SOLO en este proceso, los parámetros
    existentes del JWT para confiar en el issuer del laboratorio (el mismo que el PDP). Mantiene
    issuer exacto, firma (JWKS del descubrimiento OIDC), timestamps, audiencia asistencias-api y el
    claim UUID idUsuario obligatorio. Es configuración exclusiva de laboratorio.

    -PepDisabled (SEC-BASELINE-003) arranca el MISMO jar con el flag PEP OFF (valor por defecto de
    application.yml) para comprobar que el comportamiento estable no cambia. No lee lab.env.

    -DatasourceEnv (SEC-BASELINE-003) carga DESPUÉS del .env del backend un archivo con
    SPRING_DATASOURCE_URL/USERNAME/PASSWORD de la DB E2E aislada (nunca el SQL principal). El
    archivo vive fuera de Git (%USERPROFILE%\.asistencias-security-lab\e2e-sql-*.env).
#>
[CmdletBinding()]
param(
    [int]$Port = 8083,
    [string]$BackendEnv = (Join-Path $env:USERPROFILE 'AsistenciasUCO\AsistenciasUCO\.env'),
    [string]$PepUrl = 'http://127.0.0.1:18081',
    [string]$ApplicationName = 'asistencias-uco-lab',
    [string]$LogDirectory = (Join-Path $env:TEMP 'asistencias-security-lab'),
    [ValidateSet('Institutional', 'SharedLab')]
    [string]$IdentityMode = 'Institutional',
    [string]$LabIssuer = 'http://keycloak.localhost:19090/realms/security-baseline',
    [switch]$PepDisabled,
    [string]$DatasourceEnv
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

function Import-ProcessEnvFile([string]$Path) {
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith('#')) { continue }
        $index = $trimmed.IndexOf('=')
        if ($index -lt 1) { continue }
        [Environment]::SetEnvironmentVariable($trimmed.Substring(0, $index).Trim(), $trimmed.Substring($index + 1).Trim(), 'Process')
    }
}

Import-ProcessEnvFile $BackendEnv
$datasource = 'SQL del .env del backend'
if ($DatasourceEnv) {
    Import-ProcessEnvFile $DatasourceEnv
    foreach ($key in 'SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME', 'SPRING_DATASOURCE_PASSWORD') {
        if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($key, 'Process'))) { throw "$DatasourceEnv no define $key" }
    }
    if ($env:SPRING_DATASOURCE_URL -match '(localhost|127\.0\.0\.1):1433([;/]|$)') { throw 'DatasourceEnv apunta al SQL principal (1433); se rechaza.' }
    if ($env:SPRING_DATASOURCE_USERNAME -eq 'sa') { throw 'DatasourceEnv usa sa; el backend experimental debe usar un login de minimo privilegio.' }
    $datasource = "DB aislada ($($env:SPRING_DATASOURCE_USERNAME) @ $(($env:SPRING_DATASOURCE_URL -split ';')[0]))"
}

New-Item -ItemType Directory -Force -Path $LogDirectory | Out-Null
$env:SERVER_PORT = "$Port"
if ($PepDisabled) {
    $env:SECURITY_BASELINE_PEP_ENABLED = 'false'
    $PepUrl = 'OFF'
} else {
    $lab = Import-LabEnv
    if ([string]::IsNullOrWhiteSpace($lab['SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL'])) {
        throw 'lab.env no contiene SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL (registre la aplicacion en el PDP).'
    }
    $env:SECURITY_BASELINE_PEP_ENABLED = 'true'
    $env:SECURITY_BASELINE_PEP_URL = $PepUrl
    $env:SECURITY_BASELINE_PEP_APPLICATION_NAME = $ApplicationName
    $env:SECURITY_BASELINE_PEP_ENVIRONMENT = 'local'
    $env:SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL = $lab['SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL']
}
if ($IdentityMode -eq 'SharedLab') {
    # Mismos parámetros existentes de KeycloakSecurityProviderProperties; ninguna validación cambia.
    # El JDK de Windows resuelve *.localhost a loopback, donde Keycloak publica 19090: el
    # descubrimiento OIDC/JWKS usa el issuer tal cual, sin reescribir iss ni fijar un JWKS distinto.
    $env:KEYCLOAK_ISSUER_URI = $LabIssuer
    $env:KEYCLOAK_EXPECTED_AUDIENCE = 'asistencias-api'
    $env:KEYCLOAK_API_CLIENT_ID = 'asistencias-api'
    $env:KEYCLOAK_USER_ID_CLAIM = 'idUsuario'
}
$logName = if ($PepDisabled) { 'backend-pep-off-lab' } else { 'backend-pep-lab' }
$env:ASISTENCIAS_UCO_LOG_FILE = Join-Path $LogDirectory "$logName.jsonl"
$env:ASISTENCIAS_UCO_LOG_FILE_PATTERN = Join-Path $LogDirectory "$logName.%d{yyyy-MM-dd}.%i.jsonl"

Write-Host "Backend experimental: http://127.0.0.1:$Port (PEP $PepUrl, app $ApplicationName, identidad $IdentityMode, issuer $env:KEYCLOAK_ISSUER_URI, $datasource). Logs: $LogDirectory"
# PowerShell 5.1 convierte cada línea de stderr de un ejecutable nativo en ErrorRecord; con 'Stop'
# un WARNING de la JVM (p. ej. "restricted method", al cargar Netty de Azure) mataba el proceso
# cuando la salida estaba redirigida. La JVM se supervisa por su exit code, no por stderr.
$ErrorActionPreference = 'Continue'
& (Join-Path $env:JAVA_HOME 'bin\java.exe') -jar $jar.FullName
exit $LASTEXITCODE
