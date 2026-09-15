<#
.SYNOPSIS
    Diagnostica el token efectivo de asistencias-uco-frontend usando las APIs de evaluacion de Keycloak.

.DESCRIPTION
    No modifica estado. Muestra:
      - mappers efectivos relacionados con idUsuario,
      - token de ejemplo generado por Keycloak para DOCENTE y ADMIN,
      - userinfo de ejemplo para ambos usuarios.

    No imprime passwords, secretos ni JWTs.
#>

[CmdletBinding()]
param(
    [string]$EnvFile = (Join-Path (Join-Path $PSScriptRoot '..') '.env')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Import-Module (Join-Path (Join-Path $PSScriptRoot 'lib') 'KeycloakAdmin.psm1') -Force

function Get-PropertyValue {
    param(
        [Parameter(Mandatory = $true)]$Object,
        [Parameter(Mandatory = $true)][string]$Name
    )
    if ($null -eq $Object) { return $null }
    $property = $Object.PSObject.Properties[$Name]
    if ($null -eq $property) { return $null }
    return $property.Value
}

$envMap = Import-KcDotEnv -Path $EnvFile
$serverUrl = Get-KcEnvValue -EnvMap $envMap -Key 'KEYCLOAK_SERVER_URL' -Default 'http://127.0.0.1:8081'
$realmName = Get-KcEnvValue -EnvMap $envMap -Key 'KC_REALM' -Default 'asistencias-uco'
$frontendClientId = Get-KcEnvValue -EnvMap $envMap -Key 'KC_FRONTEND_CLIENT_ID' -Default 'asistencias-uco-frontend'

$bootstrapAdminUser = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_USERNAME'
$bootstrapAdminPass = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_PASSWORD'
$adminToken = Get-KcAdminToken -ServerUrl $serverUrl -Username $bootstrapAdminUser -Password $bootstrapAdminPass

$frontendClient = Find-KcClientByClientId -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -ClientId $frontendClientId
if ($null -eq $frontendClient) {
    Write-KcResult -Status 'ERROR' -Message "Client $frontendClientId no existe"
    exit 1
}

Write-Host ''
Write-Host "== Effective token diagnostics :: $frontendClientId ==" -ForegroundColor Magenta
Write-Host ''

$effectiveMappers = (Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/clients/$($frontendClient.id)/evaluate-scopes/protocol-mappers" -Token $adminToken).Body
$relatedMappers = @()
foreach ($mapper in @($effectiveMappers)) {
    if ($null -eq $mapper) { continue }
    $json = $mapper | ConvertTo-Json -Depth 20 -Compress
    if ($json -match 'idUsuario') {
        $relatedMappers += $mapper
    }
}

Write-Host 'Mappers efectivos relacionados con idUsuario:' -ForegroundColor Cyan
if ($relatedMappers.Count -eq 0) {
    Write-Host '  <NINGUNO>' -ForegroundColor Red
} else {
    $index = 0
    foreach ($mapper in $relatedMappers) {
        $index++
        Write-Host "  [$index]" -ForegroundColor DarkCyan
        Write-Host ($mapper | ConvertTo-Json -Depth 20)
    }
}
Write-Host ''

$specs = @(
    @{ Key = 'DOCENTE'; UsernameKey = 'E2E_DOCENTE_USERNAME' },
    @{ Key = 'ADMIN'; UsernameKey = 'E2E_ADMIN_USERNAME' }
)

foreach ($spec in $specs) {
    $username = Get-KcEnvValue -EnvMap $envMap -Key $spec.UsernameKey
    if ([string]::IsNullOrWhiteSpace($username)) {
        Write-Host "[$($spec.Key)] SKIPPED: falta $($spec.UsernameKey)" -ForegroundColor Yellow
        continue
    }

    $user = Find-KcUserByUsername -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -Username $username
    if ($null -eq $user) {
        Write-Host "[$($spec.Key)] ERROR: usuario $username no existe" -ForegroundColor Red
        continue
    }

    $exampleAccess = (Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/clients/$($frontendClient.id)/evaluate-scopes/generate-example-access-token?userId=$($user.id)" -Token $adminToken).Body
    $exampleUserInfo = (Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/clients/$($frontendClient.id)/evaluate-scopes/generate-example-userinfo?userId=$($user.id)" -Token $adminToken).Body

    $exampleAccessId = Get-PropertyValue -Object $exampleAccess -Name 'idUsuario'
    $exampleUserInfoId = Get-PropertyValue -Object $exampleUserInfo -Name 'idUsuario'
    $exampleSub = Get-PropertyValue -Object $exampleAccess -Name 'sub'
    $examplePreferredUsername = Get-PropertyValue -Object $exampleAccess -Name 'preferred_username'

    Write-Host "[$($spec.Key)] $username" -ForegroundColor Cyan
    Write-Host "  user UUID                    : $($user.id)"
    Write-Host "  example token sub            : $exampleSub"
    Write-Host "  example token username       : $examplePreferredUsername"
    Write-Host "  example access idUsuario     : $exampleAccessId"
    Write-Host "  example userinfo idUsuario   : $exampleUserInfoId"
    Write-Host ''
}

Write-Host 'Diagnostico terminado. No se modifico Keycloak.' -ForegroundColor Green
