<#
.SYNOPSIS
    Valida que el realm Keycloak de AsistenciasUCO cumpla el contrato canonico.

.DESCRIPTION
    Inspeccion de solo lectura (no crea/modifica nada). Verifica realm, clients, client scope,
    roles institucionales, mappers (idUsuario + audience), asignacion del scope como default,
    origenes/redirects del frontend, permisos minimos del service account de
    asistencias-backend-admin, y reporta objetos legacy encontrados.

    Termina con exit code != 0 si falta alguna pieza esencial del contrato.

.EXAMPLE
    cd infra/keycloak
    .\scripts\validate-keycloak.ps1
#>

[CmdletBinding()]
param(
    [string]$EnvFile
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    $EnvFile = Join-Path (Join-Path $PSScriptRoot '..') '.env'
}

Import-Module (Join-Path (Join-Path $PSScriptRoot 'lib') 'KeycloakAdmin.psm1') -Force

$envMap = Import-KcDotEnv -Path $EnvFile

$serverUrl        = Get-KcEnvValue -EnvMap $envMap -Key 'KEYCLOAK_SERVER_URL' -Default 'http://127.0.0.1:8081'
$realmName        = Get-KcEnvValue -EnvMap $envMap -Key 'KC_REALM' -Default 'asistencias-uco'
$apiClientId      = Get-KcEnvValue -EnvMap $envMap -Key 'KC_API_CLIENT_ID' -Default 'asistencias-api'
$frontendClientId = Get-KcEnvValue -EnvMap $envMap -Key 'KC_FRONTEND_CLIENT_ID' -Default 'asistencias-uco-frontend'
$legacyFrontendClientId = 'asistencias-frontend'
$backendAdminClientId   = Get-KcEnvValue -EnvMap $envMap -Key 'KC_BACKEND_ADMIN_CLIENT_ID' -Default 'asistencias-backend-admin'
$clientScopeName  = 'asistencias-api-scope'
$userIdAttribute  = 'idUsuario'

$bootstrapAdminUser = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_USERNAME'
$bootstrapAdminPass = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_PASSWORD'

$institutionalRoleNames = @('ADMINISTRADOR', 'DECANO', 'COORDINADOR', 'DOCENTE', 'ESTUDIANTE')
$legacyRoleCodes = @('AD', 'DE', 'CD', 'DO', 'ES')
$requiredRealmManagementRoles = @('view-users', 'manage-users', 'view-clients')

$failures = New-Object System.Collections.Generic.List[string]
$warnings = New-Object System.Collections.Generic.List[string]

function Test-Check {
    param(
        [Parameter(Mandatory = $true)][bool]$Condition,
        [Parameter(Mandatory = $true)][string]$OkMessage,
        [Parameter(Mandatory = $true)][string]$FailMessage
    )
    if ($Condition) {
        Write-KcResult -Status 'OK' -Message $OkMessage
    } else {
        Write-KcResult -Status 'ERROR' -Message $FailMessage
        $failures.Add($FailMessage) | Out-Null
    }
}

function Get-ConfigString {
    param(
        $Config,
        [Parameter(Mandatory = $true)][string]$Name
    )
    if ($null -eq $Config) { return $null }
    $property = $Config.PSObject.Properties[$Name]
    if ($null -eq $property) { return $null }
    return [string]$property.Value
}

Write-Host ''
Write-Host "== Validate Keycloak :: realm $realmName ==" -ForegroundColor Magenta
Write-Host ''

try {
    $adminToken = Get-KcAdminToken -ServerUrl $serverUrl -Username $bootstrapAdminUser -Password $bootstrapAdminPass
} catch {
    Write-KcResult -Status 'ERROR' -Message "No fue posible autenticar contra Keycloak: $($_.Exception.Message)"
    exit 1
}

$realm = Find-KcRealm -ServerUrl $serverUrl -Token $adminToken -RealmName $realmName
Test-Check -Condition ($null -ne $realm -and $realm.enabled) -OkMessage "Realm $realmName (enabled)" -FailMessage "Realm $realmName no existe o esta deshabilitado"
if ($null -eq $realm) {
    Write-Host ''
    Write-Host "$($failures.Count) fallo(s) critico(s)." -ForegroundColor Red
    exit 1
}

$hasPasswordPolicy = -not [string]::IsNullOrWhiteSpace($realm.passwordPolicy) -and $realm.passwordPolicy -match 'length\(\d+\)' -and $realm.passwordPolicy -match 'hashAlgorithm\(argon2\)'
Test-Check -Condition $hasPasswordPolicy -OkMessage 'Password policy' -FailMessage 'Password policy ausente o incompleta (se espera length/digits/lowerCase/upperCase/specialChars/notUsername/notEmail/passwordHistory/hashAlgorithm=argon2)'

$apiClient = Find-KcClientByClientId -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -ClientId $apiClientId
Test-Check -Condition ($null -ne $apiClient) -OkMessage "Client $apiClientId" -FailMessage "Client $apiClientId no existe"

if ($null -ne $apiClient) {
    Test-Check -Condition (-not $apiClient.standardFlowEnabled -and -not $apiClient.directAccessGrantsEnabled -and -not $apiClient.implicitFlowEnabled -and -not $apiClient.serviceAccountsEnabled) `
        -OkMessage "$apiClientId configurado API-only (sin flows interactivos)" `
        -FailMessage "$apiClientId tiene flows interactivos habilitados (deberia ser API-only)"

    foreach ($roleName in $institutionalRoleNames) {
        $role = Find-KcClientRole -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -ClientUuid $apiClient.id -RoleName $roleName
        Test-Check -Condition ($null -ne $role) -OkMessage "Role $roleName" -FailMessage "Role $roleName no existe en $apiClientId"
    }

    foreach ($legacyCode in $legacyRoleCodes) {
        $legacyRole = Find-KcClientRole -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -ClientUuid $apiClient.id -RoleName $legacyCode
        if ($null -ne $legacyRole) {
            $warnings.Add("Role legacy '$legacyCode' aun existe en $apiClientId (KC_REMOVE_LEGACY_ROLES=false por default)") | Out-Null
            Write-KcResult -Status 'WARN' -Message "Role legacy detectado: $legacyCode"
        }
    }
}

$frontendClient = Find-KcClientByClientId -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -ClientId $frontendClientId
Test-Check -Condition ($null -ne $frontendClient) -OkMessage "Client $frontendClientId" -FailMessage "Client $frontendClientId no existe"

if ($null -ne $frontendClient) {
    Test-Check -Condition ($frontendClient.publicClient -eq $true -and $frontendClient.standardFlowEnabled -eq $true) `
        -OkMessage "$frontendClientId es public client con Standard Flow" `
        -FailMessage "$frontendClientId no esta configurado como public client con Standard Flow"

    $expectedRedirects = @('http://localhost:4200/*', 'http://127.0.0.1:4200/*')
    $hasRedirects = @($expectedRedirects | ForEach-Object { $_ -in @($frontendClient.redirectUris) } | Where-Object { -not $_ })
    Test-Check -Condition ($hasRedirects.Count -eq 0) -OkMessage 'Redirect URIs del frontend' -FailMessage "Redirect URIs del frontend incompletos (esperado: $($expectedRedirects -join ', '))"

    $expectedOrigins = @('http://localhost:4200', 'http://127.0.0.1:4200')
    $hasOrigins = @($expectedOrigins | ForEach-Object { $_ -in @($frontendClient.webOrigins) } | Where-Object { -not $_ })
    Test-Check -Condition ($hasOrigins.Count -eq 0 -and '*' -notin @($frontendClient.webOrigins)) -OkMessage 'Web origins del frontend' -FailMessage "Web origins del frontend incompletos o usan '*' (esperado: $($expectedOrigins -join ', '))"

    if ($frontendClient.directAccessGrantsEnabled) {
        $warnings.Add("$frontendClientId tiene Direct Access Grants ON (SECURITY DEBT documentada, no es un fallo)") | Out-Null
    }
}

$legacyFrontend = Find-KcClientByClientId -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -ClientId $legacyFrontendClientId
if ($null -ne $legacyFrontend) {
    $warnings.Add("Client legacy '$legacyFrontendClientId' aun existe") | Out-Null
    Write-KcResult -Status 'WARN' -Message "Client legacy detectado: $legacyFrontendClientId"
}

$clientScope = Find-KcClientScopeByName -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -Name $clientScopeName
Test-Check -Condition ($null -ne $clientScope) -OkMessage "Client scope $clientScopeName" -FailMessage "Client scope $clientScopeName no existe"

if ($null -ne $clientScope) {
    $mappers = @((Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/client-scopes/$($clientScope.id)/protocol-mappers/models" -Token $adminToken).Body)
    $claimMappers = @($mappers | Where-Object { (Get-ConfigString -Config $_.config -Name 'claim.name') -eq $userIdAttribute })
    Test-Check -Condition ($claimMappers.Count -eq 1) -OkMessage "Exactamente un mapper para claim $userIdAttribute en $clientScopeName" -FailMessage "Se esperaba exactamente un mapper para claim $userIdAttribute en $clientScopeName y hay $($claimMappers.Count)"

    if ($claimMappers.Count -eq 1) {
        $idUsuarioMapper = $claimMappers[0]
        $mapperIsExact = $idUsuarioMapper.name -eq $userIdAttribute -and
            $idUsuarioMapper.protocol -eq 'openid-connect' -and
            $idUsuarioMapper.protocolMapper -eq 'oidc-usermodel-attribute-mapper' -and
            (Get-ConfigString -Config $idUsuarioMapper.config -Name 'user.attribute') -ceq $userIdAttribute -and
            (Get-ConfigString -Config $idUsuarioMapper.config -Name 'claim.name') -ceq $userIdAttribute -and
            (Get-ConfigString -Config $idUsuarioMapper.config -Name 'jsonType.label') -eq 'String' -and
            (Get-ConfigString -Config $idUsuarioMapper.config -Name 'access.token.claim') -eq 'true' -and
            (Get-ConfigString -Config $idUsuarioMapper.config -Name 'id.token.claim') -eq 'true' -and
            (Get-ConfigString -Config $idUsuarioMapper.config -Name 'userinfo.token.claim') -eq 'true' -and
            (Get-ConfigString -Config $idUsuarioMapper.config -Name 'introspection.token.claim') -eq 'true' -and
            (Get-ConfigString -Config $idUsuarioMapper.config -Name 'multivalued') -eq 'false'
        Test-Check -Condition $mapperIsExact -OkMessage "Mapper ${userIdAttribute}: protocolo, atributo, claim y destinos exactos" -FailMessage "Mapper $userIdAttribute no cumple exactamente protocolo/user.attribute/claim.name/String/access/ID/userinfo/introspection/multivalued=false"
    }

    $scopeMappings = (Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/client-scopes/$($clientScope.id)/scope-mappings" -Token $adminToken).Body
    $scopeMappingCount = 0
    $realmMappingsProperty = $scopeMappings.PSObject.Properties['realmMappings']
    if ($null -ne $realmMappingsProperty -and $null -ne $realmMappingsProperty.Value) {
        $scopeMappingCount += @($realmMappingsProperty.Value).Count
    }
    $clientMappingsProperty = $scopeMappings.PSObject.Properties['clientMappings']
    if ($null -ne $clientMappingsProperty -and $null -ne $clientMappingsProperty.Value) {
        foreach ($mappingProperty in $clientMappingsProperty.Value.PSObject.Properties) {
            $scopeMappingCount += @($mappingProperty.Value.mappings).Count
        }
    }
    Test-Check -Condition ($scopeMappingCount -eq 0) -OkMessage "$clientScopeName sin Role Scope Mappings (claims disponibles para todo usuario)" -FailMessage "$clientScopeName tiene $scopeMappingCount Role Scope Mapping(s): Keycloak omitira todos sus mappers para usuarios sin esos roles"

    $audienceMapper = @($mappers) | Where-Object { $_.protocolMapper -eq 'oidc-audience-mapper' -and $_.config.'included.client.audience' -eq $apiClientId }
    Test-Check -Condition (@($audienceMapper).Count -eq 1) -OkMessage "Exactamente un mapper audience $apiClientId" -FailMessage "Se esperaba exactamente un mapper audience ($apiClientId) en $clientScopeName"

    if ($null -ne $frontendClient) {
        $defaultScopes = (Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/clients/$($frontendClient.id)/default-client-scopes" -Token $adminToken).Body
        $isDefault = @($defaultScopes) | Where-Object { $_.name -eq $clientScopeName }
        Test-Check -Condition ($null -ne $isDefault) -OkMessage "$clientScopeName asignado como DEFAULT a $frontendClientId" -FailMessage "$clientScopeName no esta asignado como DEFAULT a $frontendClientId"

        $effectiveSummaries = @((Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/clients/$($frontendClient.id)/evaluate-scopes/protocol-mappers" -Token $adminToken).Body)
        $effectiveIdUsuarioMappers = New-Object System.Collections.Generic.List[object]
        foreach ($summary in $effectiveSummaries) {
            if ($null -eq $summary) { continue }
            $mapperPath = $null
            if ($summary.containerType -eq 'client-scope') {
                $mapperPath = "/admin/realms/$realmName/client-scopes/$($summary.containerId)/protocol-mappers/models/$($summary.mapperId)"
            } elseif ($summary.containerType -eq 'client') {
                $mapperPath = "/admin/realms/$realmName/clients/$($summary.containerId)/protocol-mappers/models/$($summary.mapperId)"
            }
            if ($null -eq $mapperPath) { continue }
            $effectiveMapper = (Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path $mapperPath -Token $adminToken -AllowNotFound).Body
            if ($null -ne $effectiveMapper -and (Get-ConfigString -Config $effectiveMapper.config -Name 'claim.name') -eq $userIdAttribute) {
                $effectiveIdUsuarioMappers.Add($effectiveMapper) | Out-Null
            }
        }
        Test-Check -Condition ($effectiveIdUsuarioMappers.Count -eq 1) -OkMessage "Exactamente un mapper efectivo para claim $userIdAttribute en $frontendClientId" -FailMessage "Se esperaba exactamente un mapper efectivo para claim $userIdAttribute en $frontendClientId y hay $($effectiveIdUsuarioMappers.Count)"
    }
}

$backendAdminClient = Find-KcClientByClientId -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -ClientId $backendAdminClientId
Test-Check -Condition ($null -ne $backendAdminClient) -OkMessage "Client $backendAdminClientId" -FailMessage "Client $backendAdminClientId no existe"

if ($null -ne $backendAdminClient) {
    Test-Check -Condition ($backendAdminClient.publicClient -eq $false -and $backendAdminClient.serviceAccountsEnabled -eq $true -and -not $backendAdminClient.standardFlowEnabled -and -not $backendAdminClient.directAccessGrantsEnabled) `
        -OkMessage "$backendAdminClientId configurado como confidential + service account" `
        -FailMessage "$backendAdminClientId no esta configurado como confidential/service-account-only"

    $serviceAccountUser = (Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/clients/$($backendAdminClient.id)/service-account-user" -Token $adminToken -AllowNotFound).Body
    if ($null -ne $serviceAccountUser) {
        $realmManagementClient = Find-KcClientByClientId -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -ClientId 'realm-management'
        if ($null -ne $realmManagementClient) {
            $assignedRoles = (Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users/$($serviceAccountUser.id)/role-mappings/clients/$($realmManagementClient.id)" -Token $adminToken).Body
            $assignedNames = @($assignedRoles) | ForEach-Object { $_.name }
            foreach ($required in $requiredRealmManagementRoles) {
                Test-Check -Condition ($assignedNames -contains $required) -OkMessage "Service account: $required" -FailMessage "Service account de $backendAdminClientId no tiene el role realm-management '$required'"
            }
            $excessive = @(@('realm-admin', 'manage-realm', 'manage-clients', 'manage-authorization') | Where-Object { $assignedNames -contains $_ })
            if ($excessive.Count -gt 0) {
                $warnings.Add("Service account de $backendAdminClientId tiene permisos excesivos: $($excessive -join ', ')") | Out-Null
                Write-KcResult -Status 'WARN' -Message "Permisos excesivos detectados: $($excessive -join ', ')"
            }
        }
    } else {
        $failures.Add("$backendAdminClientId no tiene service account habilitado") | Out-Null
        Write-KcResult -Status 'ERROR' -Message "$backendAdminClientId no tiene service account habilitado"
    }
}

$userProfile = (Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users/profile" -Token $adminToken -AllowNotFound).Body
Test-Check -Condition ($null -ne $userProfile) -OkMessage 'User Profile disponible' -FailMessage 'No fue posible leer User Profile'
if ($null -ne $userProfile) {
    $idUsuarioAttributes = @($userProfile.attributes | Where-Object { $_.name -ceq $userIdAttribute })
    Test-Check -Condition ($idUsuarioAttributes.Count -eq 1) -OkMessage "User Profile declara exactamente un atributo $userIdAttribute" -FailMessage "User Profile debe declarar exactamente un atributo $userIdAttribute con el case exacto"
    if ($idUsuarioAttributes.Count -eq 1) {
        $attribute = $idUsuarioAttributes[0]
        $view = @()
        $edit = @()
        $permissionsProperty = $attribute.PSObject.Properties['permissions']
        if ($null -ne $permissionsProperty -and $null -ne $permissionsProperty.Value) {
            $viewProperty = $permissionsProperty.Value.PSObject.Properties['view']
            $editProperty = $permissionsProperty.Value.PSObject.Properties['edit']
            if ($null -ne $viewProperty) { $view = @($viewProperty.Value) }
            if ($null -ne $editProperty) { $edit = @($editProperty.Value) }
        }
        $permissionsOk = $view.Count -eq 2 -and $view -contains 'user' -and $view -contains 'admin' -and $edit.Count -eq 1 -and $edit -contains 'admin'
        Test-Check -Condition $permissionsOk -OkMessage "$userIdAttribute permisos view=user,admin; edit=admin" -FailMessage "$userIdAttribute debe ser visible por user/admin y editable solo por admin"

        $multivaluedProperty = $attribute.PSObject.Properties['multivalued']
        $singleValued = $null -ne $multivaluedProperty -and $multivaluedProperty.Value -eq $false
        Test-Check -Condition $singleValued -OkMessage "$userIdAttribute single-valued" -FailMessage "$userIdAttribute debe tener multivalued=false"

        $displayNameProperty = $attribute.PSObject.Properties['displayName']
        $annotationsProperty = $attribute.PSObject.Properties['annotations']
        $annotationCount = 0
        if ($null -ne $annotationsProperty -and $null -ne $annotationsProperty.Value) {
            $annotationCount = @($annotationsProperty.Value.PSObject.Properties).Count
        }
        $metadataOk = $null -ne $displayNameProperty -and $displayNameProperty.Value -eq 'ID Usuario' -and
            $annotationCount -eq 0 -and
            $null -eq $attribute.PSObject.Properties['required'] -and
            $null -eq $attribute.PSObject.Properties['selector']
        Test-Check -Condition $metadataOk -OkMessage "$userIdAttribute metadata canonica (sin required/selector/annotations)" -FailMessage "$userIdAttribute debe usar displayName canonico y no tener required, selector ni annotations"

        $uuidPattern = '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
        $actualPattern = $null
        $validationsProperty = $attribute.PSObject.Properties['validations']
        if ($null -ne $validationsProperty -and $null -ne $validationsProperty.Value) {
            $patternProperty = $validationsProperty.Value.PSObject.Properties['pattern']
            if ($null -ne $patternProperty -and $null -ne $patternProperty.Value) {
                $actualPattern = [string]$patternProperty.Value.pattern
            }
        }
        Test-Check -Condition ($actualPattern -eq $uuidPattern) -OkMessage "$userIdAttribute valida formato UUID" -FailMessage "$userIdAttribute no tiene el validador UUID canonico"
    }
}

Write-Host ''
Write-Host '== Resumen ==' -ForegroundColor Magenta
Write-Host "Fallos criticos: $($failures.Count)" -ForegroundColor $(if ($failures.Count -gt 0) { 'Red' } else { 'Green' })
Write-Host "Advertencias:    $($warnings.Count)" -ForegroundColor $(if ($warnings.Count -gt 0) { 'Yellow' } else { 'Green' })
Write-Host ''

if ($failures.Count -gt 0) {
    exit 1
}
exit 0
