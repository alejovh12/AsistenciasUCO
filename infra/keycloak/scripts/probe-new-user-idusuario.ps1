<#
.SYNOPSIS
    Aisla si el mapper idUsuario falla para usuarios nuevos o solo para admin.prueba.

.DESCRIPTION
    Crea un usuario temporal sin password, usando E2E_ADMIN_ID_USUARIO como atributo idUsuario.
    Evalua el token de ejemplo antes y despues de asignar ADMINISTRADOR y elimina siempre el
    usuario temporal al finalizar. No modifica usuarios E2E existentes.
#>

[CmdletBinding()]
param(
    [string]$EnvFile = (Join-Path (Join-Path $PSScriptRoot '..') '.env')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Import-Module (Join-Path (Join-Path $PSScriptRoot 'lib') 'KeycloakAdmin.psm1') -Force

function Get-OptionalPropertyValue {
    param(
        [Parameter(Mandatory = $true)]$Object,
        [Parameter(Mandatory = $true)][string]$Name
    )
    if ($null -eq $Object) { return $null }
    $property = $Object.PSObject.Properties[$Name]
    if ($null -eq $property) { return $null }
    return $property.Value
}

function Get-ExampleIdUsuario {
    param(
        [Parameter(Mandatory = $true)][string]$ClientUuid,
        [Parameter(Mandatory = $true)][string]$UserUuid
    )

    $example = (Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/clients/$ClientUuid/evaluate-scopes/generate-example-access-token?userId=$UserUuid" -Token $adminToken).Body
    return Get-OptionalPropertyValue -Object $example -Name 'idUsuario'
}

$envMap = Import-KcDotEnv -Path $EnvFile
$serverUrl = Get-KcEnvValue -EnvMap $envMap -Key 'KEYCLOAK_SERVER_URL' -Default 'http://127.0.0.1:8081'
$realmName = Get-KcEnvValue -EnvMap $envMap -Key 'KC_REALM' -Default 'asistencias-uco'
$frontendClientId = Get-KcEnvValue -EnvMap $envMap -Key 'KC_FRONTEND_CLIENT_ID' -Default 'asistencias-uco-frontend'
$apiClientId = Get-KcEnvValue -EnvMap $envMap -Key 'KC_API_CLIENT_ID' -Default 'asistencias-api'
$probeIdUsuario = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'E2E_ADMIN_ID_USUARIO'

$bootstrapAdminUser = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_USERNAME'
$bootstrapAdminPass = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_PASSWORD'
$adminToken = Get-KcAdminToken -ServerUrl $serverUrl -Username $bootstrapAdminUser -Password $bootstrapAdminPass

$frontendClient = Find-KcClientByClientId -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -ClientId $frontendClientId
$apiClient = Find-KcClientByClientId -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -ClientId $apiClientId
if ($null -eq $frontendClient -or $null -eq $apiClient) {
    throw 'Faltan clients requeridos. Ejecuta bootstrap-keycloak.ps1 primero.'
}

$adminRole = Find-KcClientRole -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -ClientUuid $apiClient.id -RoleName 'ADMINISTRADOR'
if ($null -eq $adminRole) {
    throw 'Role ADMINISTRADOR no existe en asistencias-api.'
}

$probeUsername = "probe.idusuario.$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())"
$probeUserId = $null

Write-Host ''
Write-Host "== Probe usuario nuevo :: $probeUsername ==" -ForegroundColor Magenta
Write-Host "idUsuario de prueba: $probeIdUsuario"
Write-Host ''

try {
    $created = Invoke-KcAdminApi -Method Post -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users" -Token $adminToken -Body @{
        username      = $probeUsername
        email         = "$probeUsername@uco.local"
        firstName     = 'Probe'
        lastName      = 'IdUsuario'
        enabled       = $true
        emailVerified = $true
        attributes    = @{ idUsuario = @($probeIdUsuario) }
    }
    $probeUserId = ($created.Location -split '/')[-1]
    Write-KcResult -Status 'CREATED' -Message "Probe $probeUsername ($probeUserId)"

    $fullUser = (Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users/$probeUserId" -Token $adminToken).Body
    $stored = $null
    if ($null -ne $fullUser.PSObject.Properties['attributes'] -and $null -ne $fullUser.attributes) {
        $idProperty = $fullUser.attributes.PSObject.Properties['idUsuario']
        if ($null -ne $idProperty) {
            $values = @($idProperty.Value)
            if ($values.Count -gt 0) { $stored = [string]$values[0] }
        }
    }

    $beforeRole = Get-ExampleIdUsuario -ClientUuid $frontendClient.id -UserUuid $probeUserId
    Write-Host "Admin REST idUsuario              : $stored"
    Write-Host "Example token ANTES de role       : $beforeRole"

    Invoke-KcAdminApi -Method Post -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users/$probeUserId/role-mappings/clients/$($apiClient.id)" -Token $adminToken -Body @($adminRole) | Out-Null
    Write-KcResult -Status 'UPDATED' -Message 'Probe: role ADMINISTRADOR asignado'

    $afterRole = Get-ExampleIdUsuario -ClientUuid $frontendClient.id -UserUuid $probeUserId
    Write-Host "Example token DESPUES de role     : $afterRole"
    Write-Host ''

    if ($beforeRole -eq $probeIdUsuario -and $afterRole -eq $probeIdUsuario) {
        Write-KcResult -Status 'OK' -Message 'Usuarios nuevos SI exponen idUsuario; el problema es especifico de admin.prueba.'
    } elseif ([string]::IsNullOrWhiteSpace([string]$beforeRole)) {
        Write-KcResult -Status 'WARN' -Message 'Usuario nuevo NO expone idUsuario antes de roles; el problema no depende de ADMINISTRADOR.'
    } elseif ($beforeRole -eq $probeIdUsuario -and [string]::IsNullOrWhiteSpace([string]$afterRole)) {
        Write-KcResult -Status 'WARN' -Message 'idUsuario desaparece al asignar ADMINISTRADOR; revisar comportamiento ligado al role.'
    } else {
        Write-KcResult -Status 'WARN' -Message 'Resultado no esperado; revisar valores mostrados arriba.'
    }
}
finally {
    if (-not [string]::IsNullOrWhiteSpace([string]$probeUserId)) {
        try {
            Invoke-KcAdminApi -Method Delete -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users/$probeUserId" -Token $adminToken | Out-Null
            Write-KcResult -Status 'OK' -Message "Probe temporal eliminado: $probeUsername"
        } catch {
            Write-KcResult -Status 'ERROR' -Message "No se pudo eliminar probe temporal $probeUsername. Eliminalo manualmente antes de continuar."
        }
    }
}
