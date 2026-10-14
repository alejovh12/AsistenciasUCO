<#
.SYNOPSIS
    Genera el env-file local del laboratorio SEC-BASELINE-001 con secretos aleatorios.

.DESCRIPTION
    Escribe %USERPROFILE%\.asistencias-security-lab\lab.env (fuera de cualquier repositorio) y
    restringe su ACL al usuario actual. Si el archivo ya existe no lo toca (idempotente), salvo
    -Force. Nunca imprime los valores generados.
#>
[CmdletBinding()]
param([switch]$Force)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'LabCommon.psm1') -Force

$directory = Get-LabSecretsDirectory
$path = Get-LabEnvPath
if ((Test-Path -LiteralPath $path) -and -not $Force) {
    Write-Host "lab.env ya existe en $path; se conserva."
    return
}
New-Item -ItemType Directory -Force -Path $directory | Out-Null

$lines = @(
    '# Generado por new-lab-env.ps1. LOCAL, NO VERSIONAR, NO COMPARTIR.'
    'SECURITY_DOCKER_ORG=davidalzate'
    'SECURITY_VERSION=0.1.6'
    'KEYCLOAK_PORT=19090'
    'PDP_PORT=18080'
    'PEP_PORT=18081'
    'PDP_SECURITY_MFA_ENABLED=false'
    'SECURITY_KC_ADMIN_USERNAME=lab-admin'
    "SECURITY_KC_ADMIN_PASSWORD=$(New-LabRandomSecret)"
    'SECURITY_SURREALDB_USERNAME=root'
    "SECURITY_SURREALDB_PASSWORD=$(New-LabRandomSecret)"
    "SECURITY_PDP_KEYCLOAK_CLIENT_SECRET=$(New-LabRandomSecret)"
    "SECURITY_PEP_INTERNAL_CLIENT_SECRET=$(New-LabRandomSecret)"
)
Set-Content -LiteralPath $path -Value $lines -Encoding ascii
Protect-LabFile -Path $path
Write-Host "lab.env generado en $path (valores no mostrados)."
