Set-StrictMode -Version Latest

# Utilidades compartidas del laboratorio SEC-BASELINE-001. Nunca imprimen valores secretos.

function Get-LabSecretsDirectory {
    return (Join-Path $env:USERPROFILE '.asistencias-security-lab')
}

function Get-LabEnvPath {
    return (Join-Path (Get-LabSecretsDirectory) 'lab.env')
}

function Import-LabEnv {
    param([string]$Path = (Get-LabEnvPath))
    if (-not (Test-Path -LiteralPath $Path)) {
        throw "No existe $Path. Ejecute primero new-lab-env.ps1."
    }
    $map = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith('#')) { continue }
        $index = $trimmed.IndexOf('=')
        if ($index -lt 1) { continue }
        $map[$trimmed.Substring(0, $index).Trim()] = $trimmed.Substring($index + 1).Trim()
    }
    return $map
}

function Set-LabEnvValue {
    param([string]$Key, [string]$Value, [string]$Path = (Get-LabEnvPath))
    $lines = @(Get-Content -LiteralPath $Path)
    $found = $false
    $updated = foreach ($line in $lines) {
        if ($line -match "^\s*$([regex]::Escape($Key))\s*=") { $found = $true; "$Key=$Value" } else { $line }
    }
    if (-not $found) { $updated = @($updated) + "$Key=$Value" }
    Set-Content -LiteralPath $Path -Value $updated -Encoding ascii
}

function New-LabRandomSecret {
    param([int]$Length = 48)
    $alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'.ToCharArray()
    $bytes = New-Object byte[] ($Length)
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return -join ($bytes | ForEach-Object { $alphabet[$_ % $alphabet.Length] })
}

function Protect-LabFile {
    param([string]$Path)
    # Solo el usuario actual: sin herencia, sin otros principals. No requiere privilegios.
    & icacls $Path /inheritance:r /grant:r "$($env:USERNAME):(R,W)" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "icacls no pudo restringir $Path" }
}

function Get-LabKeycloakAdminToken {
    param([hashtable]$EnvMap, [string]$BaseUrl = 'http://127.0.0.1:19090')
    $body = @{
        grant_type = 'password'
        client_id  = 'admin-cli'
        username   = $EnvMap['SECURITY_KC_ADMIN_USERNAME']
        password   = $EnvMap['SECURITY_KC_ADMIN_PASSWORD']
    }
    $response = Invoke-RestMethod -Method Post -Uri "$BaseUrl/realms/master/protocol/openid-connect/token" `
        -ContentType 'application/x-www-form-urlencoded' -Body $body
    return $response.access_token
}

Export-ModuleMember -Function Get-LabSecretsDirectory, Get-LabEnvPath, Import-LabEnv, Set-LabEnvValue,
    New-LabRandomSecret, Protect-LabFile, Get-LabKeycloakAdminToken
