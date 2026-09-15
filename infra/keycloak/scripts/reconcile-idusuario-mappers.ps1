<#
.SYNOPSIS
    Canoniza el mapper OIDC de idUsuario en asistencias-api-scope.

.DESCRIPTION
    Mantiene un unico mapper oidc-usermodel-attribute-mapper para el claim idUsuario.
    Si existen mappers legacy adicionales que tambien escriben el mismo claim idUsuario,
    los elimina de forma segura. No toca usuarios, passwords, roles ni otros claims.

    Idempotente: una segunda ejecucion no realiza cambios.
#>

[CmdletBinding()]
param(
    [string]$EnvFile = (Join-Path (Join-Path $PSScriptRoot '..') '.env')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Import-Module (Join-Path (Join-Path $PSScriptRoot 'lib') 'KeycloakAdmin.psm1') -Force

function Get-ConfigValue {
    param(
        [Parameter(Mandatory = $true)]$Config,
        [Parameter(Mandatory = $true)][string]$Name
    )
    if ($null -eq $Config) { return $null }
    $property = $Config.PSObject.Properties[$Name]
    if ($null -eq $property) { return $null }
    return [string]$property.Value
}

$envMap = Import-KcDotEnv -Path $EnvFile
$serverUrl = Get-KcEnvValue -EnvMap $envMap -Key 'KEYCLOAK_SERVER_URL' -Default 'http://127.0.0.1:8081'
$realmName = Get-KcEnvValue -EnvMap $envMap -Key 'KC_REALM' -Default 'asistencias-uco'
$scopeName = 'asistencias-api-scope'
$mapperName = 'idUsuario'
$claimName = 'idUsuario'
$userAttribute = 'idUsuario'

$bootstrapAdminUser = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_USERNAME'
$bootstrapAdminPass = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_PASSWORD'
$adminToken = Get-KcAdminToken -ServerUrl $serverUrl -Username $bootstrapAdminUser -Password $bootstrapAdminPass

$scope = Find-KcClientScopeByName -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -Name $scopeName
if ($null -eq $scope) {
    Write-KcResult -Status 'ERROR' -Message "Client scope $scopeName no existe. Ejecuta bootstrap-keycloak.ps1 primero."
    exit 1
}

$mappers = @((Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/client-scopes/$($scope.id)/protocol-mappers/models" -Token $adminToken).Body)
$canonical = @($mappers | Where-Object { $_.name -eq $mapperName }) | Select-Object -First 1

$desiredConfig = @{
    'user.attribute'              = $userAttribute
    'claim.name'                  = $claimName
    'jsonType.label'              = 'String'
    'id.token.claim'              = 'true'
    'access.token.claim'          = 'true'
    'userinfo.token.claim'        = 'true'
    'introspection.token.claim'   = 'true'
    'multivalued'                 = 'false'
}

if ($null -eq $canonical) {
    Invoke-KcAdminApi -Method Post -ServerUrl $serverUrl -Path "/admin/realms/$realmName/client-scopes/$($scope.id)/protocol-mappers/models" -Token $adminToken -Body @{
        name           = $mapperName
        protocol       = 'openid-connect'
        protocolMapper = 'oidc-usermodel-attribute-mapper'
        config         = $desiredConfig
    } | Out-Null
    Write-KcResult -Status 'CREATED' -Message "Mapper canonico $mapperName"

    $mappers = @((Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/client-scopes/$($scope.id)/protocol-mappers/models" -Token $adminToken).Body)
    $canonical = @($mappers | Where-Object { $_.name -eq $mapperName }) | Select-Object -First 1
} else {
    $needsUpdate = $false
    if ($canonical.protocol -ne 'openid-connect') { $needsUpdate = $true }
    if ($canonical.protocolMapper -ne 'oidc-usermodel-attribute-mapper') { $needsUpdate = $true }
    foreach ($key in $desiredConfig.Keys) {
        if ((Get-ConfigValue -Config $canonical.config -Name $key) -ne [string]$desiredConfig[$key]) {
            $needsUpdate = $true
        }
    }

    if ($needsUpdate) {
        Invoke-KcAdminApi -Method Put -ServerUrl $serverUrl -Path "/admin/realms/$realmName/client-scopes/$($scope.id)/protocol-mappers/models/$($canonical.id)" -Token $adminToken -Body @{
            id             = $canonical.id
            name           = $mapperName
            protocol       = 'openid-connect'
            protocolMapper = 'oidc-usermodel-attribute-mapper'
            config         = $desiredConfig
        } | Out-Null
        Write-KcResult -Status 'UPDATED' -Message "Mapper canonico $mapperName"
    } else {
        Write-KcResult -Status 'OK' -Message "Mapper canonico $mapperName"
    }
}

$mappers = @((Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/client-scopes/$($scope.id)/protocol-mappers/models" -Token $adminToken).Body)
$canonical = @($mappers | Where-Object { $_.name -eq $mapperName }) | Select-Object -First 1

$duplicates = @()
foreach ($mapper in $mappers) {
    if ($null -eq $mapper) { continue }
    if ($mapper.id -eq $canonical.id) { continue }
    if ($mapper.protocolMapper -ne 'oidc-usermodel-attribute-mapper') { continue }

    $mappedClaim = Get-ConfigValue -Config $mapper.config -Name 'claim.name'
    if ($mappedClaim -eq $claimName) {
        $duplicates += $mapper
    }
}

if ($duplicates.Count -eq 0) {
    Write-KcResult -Status 'OK' -Message 'No hay mappers duplicados para claim idUsuario'
} else {
    foreach ($duplicate in $duplicates) {
        $mappedAttribute = Get-ConfigValue -Config $duplicate.config -Name 'user.attribute'
        Write-KcResult -Status 'WARN' -Message "Mapper duplicado detectado: $($duplicate.name) (user.attribute=$mappedAttribute, claim.name=$claimName)"
        Invoke-KcAdminApi -Method Delete -ServerUrl $serverUrl -Path "/admin/realms/$realmName/client-scopes/$($scope.id)/protocol-mappers/models/$($duplicate.id)" -Token $adminToken | Out-Null
        Write-KcResult -Status 'UPDATED' -Message "Mapper duplicado eliminado: $($duplicate.name)"
    }
}

$remaining = @((Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/client-scopes/$($scope.id)/protocol-mappers/models" -Token $adminToken).Body)
$claimMappers = @($remaining | Where-Object {
    $_.protocolMapper -eq 'oidc-usermodel-attribute-mapper' -and
    (Get-ConfigValue -Config $_.config -Name 'claim.name') -eq $claimName
})

if ($claimMappers.Count -ne 1) {
    Write-KcResult -Status 'ERROR' -Message "Se esperaban 1 mapper para claim $claimName y quedaron $($claimMappers.Count)."
    exit 1
}

Write-KcResult -Status 'OK' -Message "Contrato final: 1 mapper para claim $claimName ($($claimMappers[0].name))"
