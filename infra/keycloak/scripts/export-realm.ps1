<#
.SYNOPSIS
    Exporta la configuracion del realm Keycloak a un archivo local para diagnostico.

.DESCRIPTION
    Usa el endpoint de partial-export de la Admin REST API (realm + clients + roles, SIN
    usuarios ni credenciales). El resultado se guarda en infra/keycloak/generated/, una ruta
    ignorada por Git — nunca se versiona.

    Este export NO es un mecanismo de backup consistente de un servidor Keycloak activo: es
    unicamente una foto de configuracion para comparar/depurar manualmente.

.EXAMPLE
    cd infra/keycloak
    .\scripts\export-realm.ps1
#>

[CmdletBinding()]
param(
    [string]$EnvFile = (Join-Path $PSScriptRoot '..' '.env'),
    [string]$OutputDirectory = (Join-Path $PSScriptRoot '..' 'generated')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Import-Module (Join-Path $PSScriptRoot 'lib' 'KeycloakAdmin.psm1') -Force

$envMap = Import-KcDotEnv -Path $EnvFile

$serverUrl = Get-KcEnvValue -EnvMap $envMap -Key 'KEYCLOAK_SERVER_URL' -Default 'http://127.0.0.1:8081'
$realmName = Get-KcEnvValue -EnvMap $envMap -Key 'KC_REALM' -Default 'asistencias-uco'

$bootstrapAdminUser = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_USERNAME'
$bootstrapAdminPass = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_PASSWORD'

if (-not (Test-Path -LiteralPath $OutputDirectory)) {
    New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
}

$adminToken = Get-KcAdminToken -ServerUrl $serverUrl -Username $bootstrapAdminUser -Password $bootstrapAdminPass

$exportPath = "/admin/realms/$realmName/partial-export?exportClients=true&exportGroupsAndRoles=true"
$result = Invoke-KcAdminApi -Method Post -ServerUrl $serverUrl -Path $exportPath -Token $adminToken

$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$outputFile = Join-Path $OutputDirectory "$realmName-$timestamp.json"
$result.Body | ConvertTo-Json -Depth 50 | Set-Content -LiteralPath $outputFile -Encoding utf8

Write-KcResult -Status 'OK' -Message "Export guardado en $outputFile"
Write-Host 'Recordatorio: este export NO incluye usuarios ni credenciales, y no es un backup consistente de un servidor activo.' -ForegroundColor Yellow
