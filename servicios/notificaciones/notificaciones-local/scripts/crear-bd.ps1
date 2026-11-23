<#
.SYNOPSIS
  Crea (o elimina) la base y el login notificacionesdb en el SQL Server del laboratorio.
.DESCRIPTION
  Ejecuta crear-bd.sql con sqlcmd dentro del contenedor de SQL Server. Es idempotente: al repetirlo solo reasigna la clave del login.
  Las claves se leen de variables de entorno o de .env (../.env); nunca se imprimen ni se pasan como argumento (-P):
    MSSQL_SA_PASSWORD            clave de sa (AsistenciasUCO-Infra/stack/.env)
    NOTIFICACIONES_DB_PASSWORD   clave del login notificacionesdb (la misma de NOTIFICACIONES_DB_PASSWORD del servicio)
.PARAMETER Contenedor
  Contenedor de SQL Server (por defecto sql_server_asistencias).
.PARAMETER Eliminar
  Elimina la base y el login en lugar de crearlos. Exige -Yes.
.PARAMETER Yes
  Confirma la eliminacion.
.EXAMPLE
  pwsh scripts/crear-bd.ps1
#>
[CmdletBinding()]
param(
    [string]$Contenedor = 'sql_server_asistencias',
    [switch]$Eliminar,
    [switch]$Yes
)

$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot

function Read-DotEnvValue {
    param([string]$Name)
    $valor = [Environment]::GetEnvironmentVariable($Name)
    if (-not [string]::IsNullOrWhiteSpace($valor)) { return $valor }
    foreach ($archivo in @((Join-Path $raiz '.env'), (Join-Path $raiz '..\..\..\AsistenciasUCO-Infra\stack\.env'))) {
        if (-not (Test-Path $archivo)) { continue }
        foreach ($linea in Get-Content -LiteralPath $archivo) {
            if ($linea -match "^\s*$Name\s*=\s*(.*)$") { return $Matches[1].Trim().Trim('"').Trim("'") }
        }
    }
    return $null
}

$sa = Read-DotEnvValue 'MSSQL_SA_PASSWORD'
if (-not $sa) { throw 'Falta MSSQL_SA_PASSWORD (variable de entorno o AsistenciasUCO-Infra/stack/.env).' }
$clave = Read-DotEnvValue 'NOTIFICACIONES_DB_PASSWORD'
if (-not $Eliminar -and (-not $clave -or $clave -like '<*')) {
    throw 'Falta NOTIFICACIONES_DB_PASSWORD (variable de entorno o notificaciones-local/.env).'
}
if ($Eliminar -and -not $Yes) { throw 'Eliminar borra la base y su cola: repita con -Yes para confirmar.' }

$estado = docker inspect -f '{{.State.Running}}' $Contenedor 2>$null
if ($estado -ne 'true') { throw "El contenedor $Contenedor no esta en ejecucion (infra.ps1 stack up minimo)." }

$sql = Join-Path $PSScriptRoot ($(if ($Eliminar) { 'eliminar-bd.sql' } else { 'crear-bd.sql' }))
$destino = '/tmp/notificaciones-bd.sql'
# El script se copia al contenedor (sin pasar por la tuberia de PowerShell: evita problemas de codificacion/BOM). Las claves
# viajan por el entorno del proceso docker (-e NOMBRE sin valor): no aparecen en la linea de comandos.
$env:SQLCMDPASSWORD = $sa
$env:NOTIFICACIONES_DB_PASSWORD = $clave
try {
    docker cp $sql "${Contenedor}:$destino"
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo copiar el script al contenedor.' }
    docker exec -e SQLCMDPASSWORD -e NOTIFICACIONES_DB_PASSWORD $Contenedor /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -C -b -i $destino
    if ($LASTEXITCODE -ne 0) { throw "sqlcmd termino con codigo $LASTEXITCODE." }
}
finally {
    docker exec -u 0 $Contenedor rm -f $destino 2>$null | Out-Null
    Remove-Item Env:\SQLCMDPASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:\NOTIFICACIONES_DB_PASSWORD -ErrorAction SilentlyContinue
}
Write-Host ($(if ($Eliminar) { 'Base notificacionesdb eliminada.' } else { 'Base notificacionesdb lista (login notificacionesdb, db_owner).' }))
