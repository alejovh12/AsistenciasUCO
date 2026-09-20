# KeycloakAdmin.psm1
#
# Capa reutilizable minima para los scripts de bootstrap/seed/validate/export de
# infra/keycloak. NO es un framework de proposito general: solo abstrae lo que
# bootstrap-keycloak.ps1, seed-e2e-users.ps1, get-test-token.ps1, validate-keycloak.ps1 y
# export-realm.ps1 necesitan en comun.
#
# Nunca imprime: admin password, client secret, access token, refresh token, passwords de
# usuario. Los helpers de logging solo aceptan mensajes ya redactados por el llamador.

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Import-KcDotEnv {
    <#
        Carga un archivo .env estilo KEY=VALUE en un hashtable ordenado.
        Ignora lineas vacias y comentarios (#). No sobreescribe variables de entorno del
        proceso: el archivo .env es la fuente para los scripts de infra/keycloak, separada
        de las variables de entorno del sistema.
    #>
    param(
        [Parameter(Mandatory = $true)][string]$Path
    )

    $result = [ordered]@{}
    if (-not (Test-Path -LiteralPath $Path)) {
        return $result
    }

    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith('#')) {
            continue
        }
        $separatorIndex = $trimmed.IndexOf('=')
        if ($separatorIndex -lt 1) {
            continue
        }
        $key = $trimmed.Substring(0, $separatorIndex).Trim()
        $value = $trimmed.Substring($separatorIndex + 1).Trim()
        if ($value.Length -ge 2 -and (
            ($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))
        )) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        $result[$key] = $value
    }

    return $result
}

function Get-KcEnvValue {
    <#
        Resuelve una variable con esta prioridad: entorno del proceso > .env cargado > default.
        Centraliza la regla para que ningun script duplique el orden de resolucion.
    #>
    param(
        [Parameter(Mandatory = $true)][hashtable]$EnvMap,
        [Parameter(Mandatory = $true)][string]$Key,
        [string]$Default = $null
    )

    $fromProcess = [System.Environment]::GetEnvironmentVariable($Key)
    if (-not [string]::IsNullOrWhiteSpace($fromProcess)) {
        return $fromProcess
    }
    if ($EnvMap.Contains($Key) -and -not [string]::IsNullOrWhiteSpace($EnvMap[$Key])) {
        return $EnvMap[$Key]
    }
    return $Default
}

function Get-KcRequiredEnvValue {
    <#
        Igual que Get-KcEnvValue pero falla con un mensaje claro (sin exponer el valor) si la
        variable no esta definida o esta vacia. Usar para secretos obligatorios.
    #>
    param(
        [Parameter(Mandatory = $true)][hashtable]$EnvMap,
        [Parameter(Mandatory = $true)][string]$Key
    )

    $value = Get-KcEnvValue -EnvMap $EnvMap -Key $Key
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "Falta la variable requerida '$Key'. Definela en infra/keycloak/.env antes de ejecutar este script."
    }
    return $value
}

function ConvertTo-KcUrlEncoded {
    param([Parameter(Mandatory = $true)][string]$Value)
    return [System.Uri]::EscapeDataString($Value)
}

function Get-KcAdminToken {
    <#
        Obtiene un access token del realm master via password grant + admin-cli, usando el
        admin de bootstrap del propio contenedor (KC_BOOTSTRAP_ADMIN_USERNAME/PASSWORD).
        Este token solo se usa localmente en memoria durante la ejecucion del script; nunca se
        imprime ni se persiste.
    #>
    param(
        [Parameter(Mandatory = $true)][string]$ServerUrl,
        [Parameter(Mandatory = $true)][string]$Username,
        [Parameter(Mandatory = $true)][string]$Password
    )

    $tokenUrl = "$ServerUrl/realms/master/protocol/openid-connect/token"
    $bodyPairs = @(
        "grant_type=password",
        "client_id=admin-cli",
        "username=$(ConvertTo-KcUrlEncoded $Username)",
        "password=$(ConvertTo-KcUrlEncoded $Password)"
    )
    $body = [string]::Join('&', $bodyPairs)

    try {
        $response = Invoke-RestMethod -Method Post -Uri $tokenUrl -ContentType 'application/x-www-form-urlencoded' -Body $body
    } catch {
        throw "No fue posible autenticar contra Keycloak ($ServerUrl) como admin de bootstrap. Verifica que Keycloak este arriba y que KC_BOOTSTRAP_ADMIN_USERNAME/PASSWORD sean correctos. Detalle: $($_.Exception.Message)"
    }

    if (-not $response.access_token) {
        throw "Keycloak respondio sin 'access_token' al autenticar el admin de bootstrap."
    }

    return $response.access_token
}

function Test-KcServerReady {
    param([Parameter(Mandatory = $true)][string]$ManagementUrl)

    try {
        $response = Invoke-WebRequest -Method Get -Uri "$ManagementUrl/health/ready" -UseBasicParsing -TimeoutSec 5
        return $response.StatusCode -eq 200
    } catch {
        return $false
    }
}

function Invoke-KcAdminApi {
    <#
        Wrapper generico para llamar a la Admin REST API (/admin/realms/... o cualquier ruta
        absoluta bajo ServerUrl). Devuelve $null en 204, el objeto deserializado en 2xx con
        cuerpo, y lanza una excepcion con mensaje redactado en cualquier otro caso.

        -Path acepta una ruta relativa a ServerUrl (debe empezar con '/').
        -AllowNotFound hace que un 404 devuelva $null en lugar de lanzar (util para "buscar si
        existe").

        Implementado con Invoke-WebRequest (no Invoke-RestMethod) a proposito: los parametros
        -SkipHttpErrorCheck/-StatusCodeVariable/-ResponseHeadersVariable solo existen en
        PowerShell 7.x y este modulo debe funcionar igual en Windows PowerShell 5.1.
    #>
    param(
        [Parameter(Mandatory = $true)][ValidateSet('Get', 'Post', 'Put', 'Delete')][string]$Method,
        [Parameter(Mandatory = $true)][string]$ServerUrl,
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Token,
        $Body = $null,
        [switch]$AllowNotFound
    )

    $uri = "$ServerUrl$Path"
    $headers = @{ Authorization = "Bearer $Token" }

    $requestArgs = @{
        Method          = $Method
        Uri             = $uri
        Headers         = $headers
        ContentType     = 'application/json; charset=utf-8'
        UseBasicParsing = $true
    }
    if ($null -ne $Body) {
        # -InputObject (nunca por pipeline): piped un array de un solo elemento se aplana y
        # ConvertTo-Json lo serializa como objeto suelto en vez de arreglo JSON de un elemento
        # (relevante para los POST de role-mappings, que siempre esperan un arreglo).
        $requestArgs['Body'] = (ConvertTo-Json -InputObject $Body -Depth 20 -Compress)
    }

    $statusCode = $null
    try {
        $response = Invoke-WebRequest @requestArgs
        $statusCode = [int]$response.StatusCode
    } catch {
        $errorResponse = $_.Exception.Response
        if ($errorResponse) {
            try { $statusCode = [int]$errorResponse.StatusCode } catch { $statusCode = $null }
        }
        if ($statusCode -eq 404 -and $AllowNotFound) {
            return $null
        }
        $detail = $_.ErrorDetails.Message
        if ([string]::IsNullOrWhiteSpace($detail)) { $detail = $_.Exception.Message }
        throw "Keycloak Admin API respondio $statusCode en $Method $Path. Detalle: $detail"
    }

    $parsedBody = $null
    if ($response.Content -and $response.Content.Trim().Length -gt 0) {
        $parsedBody = $response.Content | ConvertFrom-Json
    }

    $location = $null
    if ($response.Headers -and $response.Headers.ContainsKey('Location')) {
        $rawLocation = $response.Headers['Location']
        $location = if ($rawLocation -is [array]) { $rawLocation[0] } else { $rawLocation }
    }

    return [pscustomobject]@{
        StatusCode = $statusCode
        Body       = $parsedBody
        Location   = $location
    }
}

function Find-KcRealm {
    param(
        [Parameter(Mandatory = $true)][string]$ServerUrl,
        [Parameter(Mandatory = $true)][string]$Token,
        [Parameter(Mandatory = $true)][string]$RealmName
    )

    $result = Invoke-KcAdminApi -Method Get -ServerUrl $ServerUrl -Path "/admin/realms/$(ConvertTo-KcUrlEncoded $RealmName)" -Token $Token -AllowNotFound
    if ($null -eq $result) { return $null }
    return $result.Body
}

function Find-KcClientByClientId {
    param(
        [Parameter(Mandatory = $true)][string]$ServerUrl,
        [Parameter(Mandatory = $true)][string]$Realm,
        [Parameter(Mandatory = $true)][string]$Token,
        [Parameter(Mandatory = $true)][string]$ClientId
    )

    $result = Invoke-KcAdminApi -Method Get -ServerUrl $ServerUrl -Path "/admin/realms/$Realm/clients?clientId=$(ConvertTo-KcUrlEncoded $ClientId)" -Token $Token
    $matches = @($result.Body | Where-Object { $_.clientId -eq $ClientId })
    if ($matches.Count -eq 0) { return $null }
    if ($matches.Count -gt 1) { throw "Se encontro mas de un client con clientId '$ClientId'. Resuelve la ambiguedad manualmente antes de continuar." }
    return $matches[0]
}

function Find-KcClientRole {
    param(
        [Parameter(Mandatory = $true)][string]$ServerUrl,
        [Parameter(Mandatory = $true)][string]$Realm,
        [Parameter(Mandatory = $true)][string]$Token,
        [Parameter(Mandatory = $true)][string]$ClientUuid,
        [Parameter(Mandatory = $true)][string]$RoleName
    )

    $result = Invoke-KcAdminApi -Method Get -ServerUrl $ServerUrl -Path "/admin/realms/$Realm/clients/$ClientUuid/roles/$(ConvertTo-KcUrlEncoded $RoleName)" -Token $Token -AllowNotFound
    if ($null -eq $result) { return $null }
    return $result.Body
}

function Find-KcClientScopeByName {
    param(
        [Parameter(Mandatory = $true)][string]$ServerUrl,
        [Parameter(Mandatory = $true)][string]$Realm,
        [Parameter(Mandatory = $true)][string]$Token,
        [Parameter(Mandatory = $true)][string]$Name
    )

    $result = Invoke-KcAdminApi -Method Get -ServerUrl $ServerUrl -Path "/admin/realms/$Realm/client-scopes" -Token $Token
    $matches = @($result.Body | Where-Object { $_.name -eq $Name })
    if ($matches.Count -eq 0) { return $null }
    return $matches[0]
}

function Find-KcUserByUsername {
    param(
        [Parameter(Mandatory = $true)][string]$ServerUrl,
        [Parameter(Mandatory = $true)][string]$Realm,
        [Parameter(Mandatory = $true)][string]$Token,
        [Parameter(Mandatory = $true)][string]$Username
    )

    $result = Invoke-KcAdminApi -Method Get -ServerUrl $ServerUrl -Path "/admin/realms/$Realm/users?username=$(ConvertTo-KcUrlEncoded $Username)&exact=true" -Token $Token
    $matches = @($result.Body | Where-Object { $_.username -eq $Username })
    if ($matches.Count -eq 0) { return $null }
    return $matches[0]
}

function Write-KcResult {
    <#
        Formato de salida estandar para todos los scripts: "[STATUS] mensaje".
        STATUS tipico: OK, CREATED, UPDATED, MIGRATED, SKIPPED, WARN, ERROR.
        El llamador es responsable de no pasar secretos en $Message.
    #>
    param(
        [Parameter(Mandatory = $true)][string]$Status,
        [Parameter(Mandatory = $true)][string]$Message
    )

    $color = switch ($Status) {
        'OK' { 'Green' }
        'CREATED' { 'Cyan' }
        'UPDATED' { 'Cyan' }
        'MIGRATED' { 'Cyan' }
        'SKIPPED' { 'DarkYellow' }
        'WARN' { 'Yellow' }
        'ERROR' { 'Red' }
        default { 'White' }
    }
    Write-Host "[$Status] $Message" -ForegroundColor $color
}

Export-ModuleMember -Function @(
    'Import-KcDotEnv',
    'Get-KcEnvValue',
    'Get-KcRequiredEnvValue',
    'ConvertTo-KcUrlEncoded',
    'Get-KcAdminToken',
    'Test-KcServerReady',
    'Invoke-KcAdminApi',
    'Find-KcRealm',
    'Find-KcClientByClientId',
    'Find-KcClientRole',
    'Find-KcClientScopeByName',
    'Find-KcUserByUsername',
    'Write-KcResult'
)
