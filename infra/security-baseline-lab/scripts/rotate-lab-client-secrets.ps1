<#
.SYNOPSIS
    Sustituye en el Keycloak del laboratorio los secretos literales del realm upstream.

.DESCRIPTION
    El realm importado por la imagen security-keycloak-local trae secretos literales para los
    clientes confidenciales security-baseline-bff y security-pep-internal. Este script escribe en
    esos clientes los valores generados en lab.env, verifica la coincidencia sin imprimirlos y
    deshabilita el usuario de demostracion cuya contrasena es publica en el README upstream.
    Requiere que solo Keycloak este arriba (paso 1 del runbook) o que PDP/PEP se reinicien despues.
#>
[CmdletBinding()]
param([string]$BaseUrl = 'http://127.0.0.1:19090')

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'LabCommon.psm1') -Force

$envMap = Import-LabEnv
$token = Get-LabKeycloakAdminToken -EnvMap $envMap -BaseUrl $BaseUrl
$headers = @{ Authorization = "Bearer $token" }
$realmUrl = "$BaseUrl/admin/realms/security-baseline"

$targets = [ordered]@{
    'security-baseline-bff' = 'SECURITY_PDP_KEYCLOAK_CLIENT_SECRET'
    'security-pep-internal' = 'SECURITY_PEP_INTERNAL_CLIENT_SECRET'
}
foreach ($clientId in $targets.Keys) {
    $desired = $envMap[$targets[$clientId]]
    if ([string]::IsNullOrWhiteSpace($desired) -or $desired.Length -lt 32) {
        throw "$($targets[$clientId]) ausente o demasiado corto en lab.env"
    }
    $clients = @(Invoke-RestMethod -Headers $headers -Uri "$realmUrl/clients?clientId=$clientId")
    if ($clients.Count -ne 1) { throw "Cliente $clientId no encontrado de forma univoca" }
    $representation = Invoke-RestMethod -Headers $headers -Uri "$realmUrl/clients/$($clients[0].id)"
    $representation.secret = $desired
    Invoke-RestMethod -Method Put -Headers $headers -ContentType 'application/json' `
        -Uri "$realmUrl/clients/$($clients[0].id)" -Body ($representation | ConvertTo-Json -Depth 20) | Out-Null
    $stored = Invoke-RestMethod -Headers $headers -Uri "$realmUrl/clients/$($clients[0].id)/client-secret"
    $status = if ($stored.value -eq $desired) { 'ROTATED_AND_VERIFIED' } else { 'MISMATCH' }
    Write-Host "$clientId -> $status"
    if ($status -ne 'ROTATED_AND_VERIFIED') { exit 1 }
}

$demo = @(Invoke-RestMethod -Headers $headers -Uri "$realmUrl/users?username=security-demo&exact=true")
if ($demo.Count -eq 1 -and $demo[0].enabled) {
    Invoke-RestMethod -Method Put -Headers $headers -ContentType 'application/json' `
        -Uri "$realmUrl/users/$($demo[0].id)" -Body (@{ enabled = $false } | ConvertTo-Json) | Out-Null
    Write-Host 'security-demo -> DISABLED (credencial publica upstream)'
} else {
    Write-Host 'security-demo -> ya deshabilitado o inexistente'
}
