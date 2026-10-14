<#
.SYNOPSIS
    SEC-BASELINE-003 - crea (idempotente) el SQL Server E2E AISLADO del laboratorio.

.DESCRIPTION
    Contenedor asistencias-e2e-sqlserver, volumen asistencias-e2e-sqlserver-data, red
    asistencias-e2e-sql-net y puerto 127.0.0.1:14333 exclusivos. Usa la MISMA imagen local que el
    principal (sin pull) para que la restauración sea compatible. No toca sql_server_asistencias, su
    volumen ni su red. Secretos en %USERPROFILE%\.asistencias-security-lab\e2e-sql.env (ACL usuario).
    Si el contenedor ya existe, solo comprueba su configuración y lo arranca si está detenido.
#>
[CmdletBinding()]
param([string]$Image = 'mcr.microsoft.com/mssql/server:2022-latest')

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'LabCommon.psm1') -Force
Import-Module (Join-Path $PSScriptRoot 'E2eSql.psm1') -Force
$s = Get-E2eSqlSettings
$secrets = Import-KeyValueFile $s.SecretsPath

$principalImage = (docker inspect $s.PrincipalContainer --format '{{.Image}}').Trim()
$imageId = (docker image inspect $Image --format '{{.Id}}').Trim()
if ($principalImage -ne $imageId) { throw "La imagen local $Image no es la del principal; no se hace pull." }

$exists = (docker ps -a --filter "name=^$($s.Container)$" --format '{{.Names}}')
if (-not $exists) {
    if (-not (docker network ls --filter "name=^$($s.Network)$" --format '{{.Name}}')) {
        docker network create --label sec-baseline=003 $s.Network | Out-Null
    }
    if (-not (docker volume ls --filter "name=^$($s.Volume)$" --format '{{.Name}}')) {
        docker volume create --label sec-baseline=003 $s.Volume | Out-Null
    }
    # Archivo temporal con SOLO las variables del motor (no las contraseñas de los logins E2E).
    $containerEnv = Join-Path (Get-LabSecretsDirectory) 'e2e-sql-container.env'
    Set-Content -LiteralPath $containerEnv -Encoding ascii -Value @(
        'ACCEPT_EULA=Y', 'MSSQL_PID=Developer', "MSSQL_SA_PASSWORD=$($secrets['MSSQL_SA_PASSWORD'])")
    Protect-LabFile $containerEnv
    docker run -d --name $s.Container --hostname asistencias-e2e-sql --label sec-baseline=003 `
        --network $s.Network -p 127.0.0.1:14333:1433 -v "$($s.Volume):/var/opt/mssql" `
        --env-file $containerEnv --restart no $Image | Out-Null
    Write-Host "Creado $($s.Container) (red $($s.Network), volumen $($s.Volume), 127.0.0.1:14333)"
} else {
    $info = docker inspect $s.Container --format '{{.Image}}|{{range .Mounts}}{{.Name}}{{end}}|{{.HostConfig.NetworkMode}}'
    if ($info -ne "$imageId|$($s.Volume)|$($s.Network)") { throw "$($s.Container) existe con otra configuración: $info" }
    docker start $s.Container | Out-Null
    Write-Host "$($s.Container) ya existía; configuración verificada"
}

$deadline = (Get-Date).AddMinutes(3)
while ($true) {
    try {
        $c = New-E2eConnection -Target E2E -User sa -Password $secrets['MSSQL_SA_PASSWORD']
        $t = Invoke-E2eSql $c "SELECT @@SERVERNAME s, CAST(SERVERPROPERTY('ProductVersion') AS nvarchar(32)) v"
        $c.Close()
        Write-Host "E2E listo: servidor=$($t.Rows[0].s) version=$($t.Rows[0].v)"
        break
    } catch {
        if ((Get-Date) -gt $deadline) { throw "El SQL E2E no respondió: $($_.Exception.Message)" }
        Start-Sleep -Seconds 3
    }
}
