<#
.SYNOPSIS
    Reconcilia los permisos del atributo idUsuario en User Profile.

.DESCRIPTION
    idUsuario debe ser visible en contexto de usuario para poder formar parte del contrato de
    identidad expuesto al frontend/backend, pero solo debe ser editable por administradores.
    El script es idempotente y no modifica usuarios, passwords, roles ni otros atributos.
#>

[CmdletBinding()]
param(
    [string]$EnvFile = (Join-Path (Join-Path $PSScriptRoot '..') '.env')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Import-Module (Join-Path (Join-Path $PSScriptRoot 'lib') 'KeycloakAdmin.psm1') -Force

$envMap = Import-KcDotEnv -Path $EnvFile
$serverUrl = Get-KcEnvValue -EnvMap $envMap -Key 'KEYCLOAK_SERVER_URL' -Default 'http://127.0.0.1:8081'
$realmName = Get-KcEnvValue -EnvMap $envMap -Key 'KC_REALM' -Default 'asistencias-uco'
$bootstrapAdminUser = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_USERNAME'
$bootstrapAdminPass = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_PASSWORD'
$userIdAttribute = 'idUsuario'

$adminToken = Get-KcAdminToken -ServerUrl $serverUrl -Username $bootstrapAdminUser -Password $bootstrapAdminPass
$userProfile = (Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users/profile" -Token $adminToken).Body
$attributes = @($userProfile.attributes)
$attribute = @($attributes | Where-Object { $_.name -eq $userIdAttribute }) | Select-Object -First 1

$desiredPermissions = [pscustomobject]@{
    view = @('user', 'admin')
    edit = @('admin')
}

if ($null -eq $attribute) {
    $attribute = [pscustomobject]@{
        name        = $userIdAttribute
        displayName = $userIdAttribute
        permissions = $desiredPermissions
        multivalued = $false
    }
    $userProfile.attributes = @($attributes) + $attribute
    Invoke-KcAdminApi -Method Put -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users/profile" -Token $adminToken -Body $userProfile | Out-Null
    Write-KcResult -Status 'CREATED' -Message 'User Profile idUsuario: view=user,admin; edit=admin'
    exit 0
}

$currentView = @()
$currentEdit = @()
if ($null -ne $attribute.PSObject.Properties['permissions'] -and $null -ne $attribute.permissions) {
    if ($null -ne $attribute.permissions.PSObject.Properties['view']) { $currentView = @($attribute.permissions.view) }
    if ($null -ne $attribute.permissions.PSObject.Properties['edit']) { $currentEdit = @($attribute.permissions.edit) }
}

$viewOk = ($currentView.Count -eq 2 -and $currentView -contains 'user' -and $currentView -contains 'admin')
$editOk = ($currentEdit.Count -eq 1 -and $currentEdit -contains 'admin')

if ($viewOk -and $editOk) {
    Write-KcResult -Status 'OK' -Message 'User Profile idUsuario: view=user,admin; edit=admin'
    exit 0
}

$attribute | Add-Member -NotePropertyName permissions -NotePropertyValue $desiredPermissions -Force
$userProfile.attributes = @($attributes)
Invoke-KcAdminApi -Method Put -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users/profile" -Token $adminToken -Body $userProfile | Out-Null
Write-KcResult -Status 'UPDATED' -Message 'User Profile idUsuario: view=user,admin; edit=admin'
