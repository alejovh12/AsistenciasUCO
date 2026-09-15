<#
.SYNOPSIS
    Crea/verifica usuarios E2E opcionales para pruebas manuales de AsistenciasUCO.

.DESCRIPTION
    NO forma parte del realm base (realm-import/asistencias-uco-realm.json). Lee los datos
    exclusivamente desde infra/keycloak/.env. Si falta E2E_<ROL>_USERNAME o
    E2E_<ROL>_ID_USUARIO para un usuario, ese usuario se omite (nunca se inventa un UUID
    institucional).

    E2E_DOCENTE_ID_USUARIO debe corresponder a un dbo.Usuario.id REAL en SQL Server si se
    quiere probar autorizacion contextual (por ejemplo, asignaciones academicas del docente).

    Idempotente: no resetea la password de un usuario E2E ya existente salvo que
    KC_RESET_EXISTING_E2E_PASSWORDS=true.

.EXAMPLE
    cd infra/keycloak
    .\scripts\seed-e2e-users.ps1
#>

[CmdletBinding()]
param(
    [string]$EnvFile = (Join-Path $PSScriptRoot '..' '.env')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Import-Module (Join-Path $PSScriptRoot 'lib' 'KeycloakAdmin.psm1') -Force

$envMap = Import-KcDotEnv -Path $EnvFile

$serverUrl   = Get-KcEnvValue -EnvMap $envMap -Key 'KEYCLOAK_SERVER_URL' -Default 'http://127.0.0.1:8081'
$realmName   = Get-KcEnvValue -EnvMap $envMap -Key 'KC_REALM' -Default 'asistencias-uco'
$apiClientId = Get-KcEnvValue -EnvMap $envMap -Key 'KC_API_CLIENT_ID' -Default 'asistencias-api'

$bootstrapAdminUser = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_USERNAME'
$bootstrapAdminPass = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_PASSWORD'

$resetExistingPasswords = (Get-KcEnvValue -EnvMap $envMap -Key 'KC_RESET_EXISTING_E2E_PASSWORDS' -Default 'false') -eq 'true'

$userSpecs = @(
    @{ Key = 'DOCENTE'; Role = 'DOCENTE' },
    @{ Key = 'ADMIN'; Role = 'ADMINISTRADOR' }
)

Write-Host ''
Write-Host "== Seed usuarios E2E :: realm $realmName ==" -ForegroundColor Magenta
Write-Host ''

$adminToken = Get-KcAdminToken -ServerUrl $serverUrl -Username $bootstrapAdminUser -Password $bootstrapAdminPass
$apiClient = Find-KcClientByClientId -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -ClientId $apiClientId
if ($null -eq $apiClient) {
    Write-KcResult -Status 'ERROR' -Message "Client $apiClientId no existe. Ejecuta bootstrap-keycloak.ps1 primero."
    exit 1
}

foreach ($spec in $userSpecs) {
    $prefix = "E2E_$($spec.Key)_"
    $username = Get-KcEnvValue -EnvMap $envMap -Key "${prefix}USERNAME"
    $password = Get-KcEnvValue -EnvMap $envMap -Key "${prefix}PASSWORD"
    $idUsuario = Get-KcEnvValue -EnvMap $envMap -Key "${prefix}ID_USUARIO"
    $roleName = $spec.Role

    if ([string]::IsNullOrWhiteSpace($username) -or [string]::IsNullOrWhiteSpace($idUsuario)) {
        Write-KcResult -Status 'SKIPPED' -Message "Usuario $($spec.Key): falta ${prefix}USERNAME y/o ${prefix}ID_USUARIO en .env"
        continue
    }
    if ([string]::IsNullOrWhiteSpace($password)) {
        Write-KcResult -Status 'SKIPPED' -Message "Usuario $($spec.Key) ($username): falta ${prefix}PASSWORD en .env"
        continue
    }

    try {
        [void][System.Guid]::Parse($idUsuario)
    } catch {
        Write-KcResult -Status 'ERROR' -Message "${prefix}ID_USUARIO ('$idUsuario') no es un UUID valido."
        continue
    }

    $email = Get-KcEnvValue -EnvMap $envMap -Key "${prefix}EMAIL" -Default "$username@example-e2e.uco.edu.co"
    $firstName = Get-KcEnvValue -EnvMap $envMap -Key "${prefix}FIRST_NAME" -Default $spec.Key
    $lastName = Get-KcEnvValue -EnvMap $envMap -Key "${prefix}LAST_NAME" -Default 'Prueba'

    $existingUser = Find-KcUserByUsername -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -Username $username
    $isNew = $false

    if ($null -eq $existingUser) {
        $created = Invoke-KcAdminApi -Method Post -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users" -Token $adminToken -Body @{
            username      = $username
            email         = $email
            firstName     = $firstName
            lastName      = $lastName
            enabled       = $true
            emailVerified = $true
            attributes    = @{ idUsuario = @($idUsuario) }
        }
        $userId = ($created.Location -split '/')[-1]
        $isNew = $true
        Write-KcResult -Status 'CREATED' -Message "Usuario $username"
    } else {
        $userId = $existingUser.id
        Write-KcResult -Status 'OK' -Message "Usuario $username"
    }

    if ($isNew -or $resetExistingPasswords) {
        Invoke-KcAdminApi -Method Put -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users/$userId/reset-password" -Token $adminToken -Body @{
            type      = 'password'
            value     = $password
            temporary = $false
        } | Out-Null
        Write-KcResult -Status 'UPDATED' -Message "Usuario ${username}: password establecida"
    } else {
        Write-KcResult -Status 'SKIPPED' -Message "Usuario ${username}: password no modificada (KC_RESET_EXISTING_E2E_PASSWORDS=false)"
    }

    $currentRoles = (Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users/$userId/role-mappings/clients/$($apiClient.id)" -Token $adminToken).Body
    $hasRole = @($currentRoles) | Where-Object { $_.name -eq $roleName }
    if (-not $hasRole) {
        $role = Find-KcClientRole -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -ClientUuid $apiClient.id -RoleName $roleName
        if ($null -eq $role) {
            Write-KcResult -Status 'ERROR' -Message "Role $roleName no existe en $apiClientId. Ejecuta bootstrap-keycloak.ps1 primero."
            continue
        }
        Invoke-KcAdminApi -Method Post -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users/$userId/role-mappings/clients/$($apiClient.id)" -Token $adminToken -Body @($role) | Out-Null
        Write-KcResult -Status 'UPDATED' -Message "Usuario ${username}: role $roleName asignado"
    } else {
        Write-KcResult -Status 'OK' -Message "Usuario ${username}: role $roleName"
    }
}

Write-Host ''
Write-Host 'Seed E2E completado.' -ForegroundColor Green
