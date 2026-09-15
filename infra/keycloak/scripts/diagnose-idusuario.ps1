<#
.SYNOPSIS
    Diagnostica por que idUsuario aparece o no en los tokens E2E sin imprimir secretos ni JWTs.

.DESCRIPTION
    Compara, para DOCENTE y ADMIN:
      - atributo idUsuario visible por Admin REST API,
      - fila persistida en PostgreSQL de Keycloak (si docker compose/psql estan disponibles),
      - claim idUsuario del access token,
      - claim idUsuario de /userinfo,
      - roles de asistencias-api.

    No modifica Keycloak, usuarios, passwords, roles ni base de datos.
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

function ConvertFrom-Base64UrlSafe {
    param([Parameter(Mandatory = $true)][string]$Value)
    $padded = $Value.Replace('-', '+').Replace('_', '/')
    switch ($padded.Length % 4) {
        2 { $padded += '==' }
        3 { $padded += '=' }
    }
    return [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($padded))
}

function Get-E2eAccessToken {
    param(
        [Parameter(Mandatory = $true)][string]$Username,
        [Parameter(Mandatory = $true)][string]$Password
    )

    $tokenUrl = "$serverUrl/realms/$realmName/protocol/openid-connect/token"
    $bodyPairs = @(
        'grant_type=password',
        "client_id=$(ConvertTo-KcUrlEncoded $frontendClientId)",
        "username=$(ConvertTo-KcUrlEncoded $Username)",
        "password=$(ConvertTo-KcUrlEncoded $Password)"
    )
    $body = [string]::Join('&', $bodyPairs)
    $response = Invoke-RestMethod -Method Post -Uri $tokenUrl -ContentType 'application/x-www-form-urlencoded' -Body $body
    return [string]$response.access_token
}

$envMap = Import-KcDotEnv -Path $EnvFile
$serverUrl = Get-KcEnvValue -EnvMap $envMap -Key 'KEYCLOAK_SERVER_URL' -Default 'http://127.0.0.1:8081'
$realmName = Get-KcEnvValue -EnvMap $envMap -Key 'KC_REALM' -Default 'asistencias-uco'
$frontendClientId = Get-KcEnvValue -EnvMap $envMap -Key 'KC_FRONTEND_CLIENT_ID' -Default 'asistencias-uco-frontend'
$apiClientId = Get-KcEnvValue -EnvMap $envMap -Key 'KC_API_CLIENT_ID' -Default 'asistencias-api'
$dbName = Get-KcEnvValue -EnvMap $envMap -Key 'KC_DB_DATABASE'
$dbUser = Get-KcEnvValue -EnvMap $envMap -Key 'KC_DB_USERNAME'

$bootstrapAdminUser = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_USERNAME'
$bootstrapAdminPass = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_PASSWORD'
$adminToken = Get-KcAdminToken -ServerUrl $serverUrl -Username $bootstrapAdminUser -Password $bootstrapAdminPass

$specs = @(
    @{ Key = 'DOCENTE'; Role = 'DOCENTE' },
    @{ Key = 'ADMIN'; Role = 'ADMINISTRADOR' }
)

Write-Host ''
Write-Host "== Diagnose idUsuario :: realm $realmName ==" -ForegroundColor Magenta
Write-Host ''

foreach ($spec in $specs) {
    $prefix = "E2E_$($spec.Key)_"
    $username = Get-KcEnvValue -EnvMap $envMap -Key "${prefix}USERNAME"
    $password = Get-KcEnvValue -EnvMap $envMap -Key "${prefix}PASSWORD"
    $expectedId = Get-KcEnvValue -EnvMap $envMap -Key "${prefix}ID_USUARIO"

    if ([string]::IsNullOrWhiteSpace($username) -or [string]::IsNullOrWhiteSpace($password)) {
        Write-Host "[$($spec.Key)] SKIPPED: faltan username/password en .env" -ForegroundColor Yellow
        continue
    }

    $user = Find-KcUserByUsername -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -Username $username
    if ($null -eq $user) {
        Write-Host "[$($spec.Key)] ERROR: usuario $username no existe" -ForegroundColor Red
        continue
    }

    $fullUser = (Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users/$($user.id)" -Token $adminToken).Body
    $adminApiId = $null
    $attributes = Get-OptionalPropertyValue -Object $fullUser -Name 'attributes'
    if ($null -ne $attributes) {
        $idProperty = $attributes.PSObject.Properties['idUsuario']
        if ($null -ne $idProperty) {
            $values = @($idProperty.Value)
            if ($values.Count -gt 0) { $adminApiId = [string]$values[0] }
        }
    }

    $accessToken = $null
    $tokenId = $null
    $roles = @()
    $userinfoId = $null
    $tokenError = $null

    try {
        $accessToken = Get-E2eAccessToken -Username $username -Password $password
        $segments = $accessToken.Split('.')
        if ($segments.Length -ge 2) {
            $claims = ConvertFrom-Base64UrlSafe $segments[1] | ConvertFrom-Json
            $tokenIdProperty = $claims.PSObject.Properties['idUsuario']
            if ($null -ne $tokenIdProperty) { $tokenId = [string]$tokenIdProperty.Value }

            $resourceAccessProperty = $claims.PSObject.Properties['resource_access']
            if ($null -ne $resourceAccessProperty -and $null -ne $resourceAccessProperty.Value) {
                $apiAccessProperty = $resourceAccessProperty.Value.PSObject.Properties[$apiClientId]
                if ($null -ne $apiAccessProperty -and $null -ne $apiAccessProperty.Value) {
                    $rolesProperty = $apiAccessProperty.Value.PSObject.Properties['roles']
                    if ($null -ne $rolesProperty) { $roles = @($rolesProperty.Value) }
                }
            }
        }

        try {
            $userinfo = Invoke-RestMethod -Method Get -Uri "$serverUrl/realms/$realmName/protocol/openid-connect/userinfo" -Headers @{ Authorization = "Bearer $accessToken" }
            $userinfoIdProperty = $userinfo.PSObject.Properties['idUsuario']
            if ($null -ne $userinfoIdProperty) { $userinfoId = [string]$userinfoIdProperty.Value }
        } catch {
            $userinfoId = '<userinfo no disponible>'
        }
    } catch {
        $tokenError = $_.Exception.Message
    }

    $dbValue = '<no verificado>'
    if (-not [string]::IsNullOrWhiteSpace($dbName) -and -not [string]::IsNullOrWhiteSpace($dbUser)) {
        try {
            $sql = "SELECT value FROM user_attribute WHERE user_id = '$($user.id)' AND name = 'idUsuario' ORDER BY value;"
            $keycloakDir = Join-Path $PSScriptRoot '..'
            Push-Location $keycloakDir
            try {
                $dbRows = @(& docker compose exec -T keycloak-db psql -U $dbUser -d $dbName -At -c $sql 2>$null)
                if ($LASTEXITCODE -eq 0) {
                    if ($dbRows.Count -eq 0) { $dbValue = '<SIN FILA>' }
                    else { $dbValue = ($dbRows -join ',') }
                } else {
                    $dbValue = '<consulta DB fallo>'
                }
            } finally {
                Pop-Location
            }
        } catch {
            $dbValue = '<consulta DB fallo>'
        }
    }

    Write-Host "[$($spec.Key)] $username" -ForegroundColor Cyan
    Write-Host "  expected .env        : $expectedId"
    Write-Host "  Admin REST attribute : $adminApiId"
    Write-Host "  PostgreSQL attribute : $dbValue"
    if ($null -ne $tokenError) {
        Write-Host "  access token         : ERROR al autenticar" -ForegroundColor Red
    } else {
        Write-Host "  access-token claim   : $tokenId"
        Write-Host "  userinfo claim       : $userinfoId"
        Write-Host "  API roles            : $($roles -join ', ')"
    }
    Write-Host ''
}

Write-Host 'Diagnostico terminado. No se imprimieron passwords, secretos ni JWTs.' -ForegroundColor Green
