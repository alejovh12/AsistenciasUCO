<#
.SYNOPSIS
    Comprueba si el claim idUsuario del docente proviene de un grupo y no del atributo del usuario.

.DESCRIPTION
    Keycloak UserAttributeMapper usa KeycloakModelUtils.resolveAttribute(...): primero busca el
    atributo en el usuario y, si no encuentra valor, puede heredarlo desde grupos. Este script
    lista los grupos directos de DOCENTE y ADMIN y cualquier atributo idUsuario presente.

    No modifica Keycloak.
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

$envMap = Import-KcDotEnv -Path $EnvFile
$serverUrl = Get-KcEnvValue -EnvMap $envMap -Key 'KEYCLOAK_SERVER_URL' -Default 'http://127.0.0.1:8081'
$realmName = Get-KcEnvValue -EnvMap $envMap -Key 'KC_REALM' -Default 'asistencias-uco'

$bootstrapAdminUser = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_USERNAME'
$bootstrapAdminPass = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_PASSWORD'
$adminToken = Get-KcAdminToken -ServerUrl $serverUrl -Username $bootstrapAdminUser -Password $bootstrapAdminPass

$specs = @(
    @{ Key = 'DOCENTE'; UsernameKey = 'E2E_DOCENTE_USERNAME' },
    @{ Key = 'ADMIN'; UsernameKey = 'E2E_ADMIN_USERNAME' }
)

Write-Host ''
Write-Host "== Diagnose idUsuario group source :: realm $realmName ==" -ForegroundColor Magenta
Write-Host ''

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

    $groups = @((Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users/$($user.id)/groups?briefRepresentation=false" -Token $adminToken).Body)

    Write-Host "[$($spec.Key)] $username" -ForegroundColor Cyan
    Write-Host "  user UUID: $($user.id)"

    if ($groups.Count -eq 0 -or ($groups.Count -eq 1 -and $null -eq $groups[0])) {
        Write-Host '  grupos   : <NINGUNO>'
        Write-Host ''
        continue
    }

    foreach ($group in $groups) {
        if ($null -eq $group) { continue }

        $groupName = Get-OptionalPropertyValue -Object $group -Name 'name'
        $groupPath = Get-OptionalPropertyValue -Object $group -Name 'path'
        $groupId = Get-OptionalPropertyValue -Object $group -Name 'id'

        $fullGroup = $group
        if (-not [string]::IsNullOrWhiteSpace([string]$groupId)) {
            try {
                $fullGroup = (Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/groups/$groupId" -Token $adminToken).Body
            } catch {
                $fullGroup = $group
            }
        }

        $groupIdUsuario = $null
        $attributes = Get-OptionalPropertyValue -Object $fullGroup -Name 'attributes'
        if ($null -ne $attributes) {
            $idProperty = $attributes.PSObject.Properties['idUsuario']
            if ($null -ne $idProperty) {
                $values = @($idProperty.Value)
                if ($values.Count -gt 0) { $groupIdUsuario = ($values -join ', ') }
            }
        }

        Write-Host "  group    : $groupName"
        Write-Host "  path     : $groupPath"
        Write-Host "  group idUsuario: $groupIdUsuario"
    }

    Write-Host ''
}

Write-Host 'Diagnostico terminado. No se modifico Keycloak.' -ForegroundColor Green
