<#
.SYNOPSIS
    SEC-BASELINE-002 — verificación de SOLO LECTURA de la identidad académica de laboratorio y
    snapshot de escrituras de la DB local.

.DESCRIPTION
    -Mode Verify   Comprueba en gestionasistenciadb (SQL Server Developer local) que cada idUsuario
                   del laboratorio existe, está activo, resuelve a exactamente un Docente y que ese
                   Docente es el titular del grupo FIX-PAG esperado. Escribe la evidencia en
                   %USERPROFILE%\.asistencias-security-lab\academic-identity-verification.json, que
                   provision_shared_identity_e2e.py exige ANTES de asociar el UUID en Keycloak 19090.
                   También guarda los datos esperados para el E2E (horarios, matrículas, sesiones).
    -Mode Snapshot Imprime un snapshot de escrituras (sys.dm_db_index_usage_stats.user_updates) y un
                   checksum de las tablas académicas protegidas, para comprobar que la matriz E2E
                   no escribe en la DB.

    Solo ejecuta SELECT. Lee usuario y password del .env del backend sin imprimirlos. No crea
    logins, no ejecuta seeds, no modifica fixtures. No hay base E2E aislada: ver
    docs/work-items/SEC-BASELINE-002-shared-identity-e2e/RISKS.md.
#>
[CmdletBinding()]
param(
    [ValidateSet('Verify', 'Snapshot')]
    [string]$Mode = 'Verify',
    [string]$BackendEnv = (Join-Path $env:USERPROFILE 'AsistenciasUCO\AsistenciasUCO\.env'),
    [string]$SqlHost = '127.0.0.1,1433',
    [string]$Database = 'gestionasistenciadb',
    [string]$OutFile = (Join-Path $env:USERPROFILE '.asistencias-security-lab\academic-identity-verification.json')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# idUsuario -> grupo del que debe ser titular (fixture DEV FIX-PAG del repo DB, MAINT-003L).
$expected = @(
    @{ IdUsuario = 'de000003-0000-4000-8000-000000000001'; Grupo = 'FIX-PAG-300 Paginacion 300 estudiantes'; GrupoId = 'de000005-0000-4000-8000-000000000001' },
    @{ IdUsuario = 'de000003-0000-4000-8000-000000000002'; Grupo = 'FIX-PAG-1000 Paginacion 1000 estudiantes'; GrupoId = 'de000005-0000-4000-8000-000000000002' },
    @{ IdUsuario = 'de000003-0000-4000-8000-000000000003'; Grupo = 'FIX-PAG-5000 Paginacion 5000 estudiantes'; GrupoId = 'de000005-0000-4000-8000-000000000003' }
)

$envMap = @{}
foreach ($line in Get-Content -LiteralPath $BackendEnv) {
    $t = $line.Trim()
    if ($t.Length -eq 0 -or $t.StartsWith('#') -or -not $t.Contains('=')) { continue }
    $i = $t.IndexOf('=')
    $envMap[$t.Substring(0, $i).Trim()] = $t.Substring($i + 1).Trim()
}
$builder = New-Object System.Data.SqlClient.SqlConnectionStringBuilder
$builder['Data Source'] = $SqlHost
$builder['Initial Catalog'] = $Database
$builder['User ID'] = $envMap['SPRING_DATASOURCE_USERNAME']
$builder['Password'] = $envMap['SPRING_DATASOURCE_PASSWORD']
$builder['Encrypt'] = $true
$builder['TrustServerCertificate'] = $true
$builder['ApplicationIntent'] = 'ReadOnly'
$builder['Application Name'] = 'SEC-BASELINE-002-readonly'

function Invoke-ReadOnlyQuery {
    param([System.Data.SqlClient.SqlConnection]$Connection, [string]$Sql, [hashtable]$Parameters = @{})
    if ($Sql -notmatch '^\s*(SET NOCOUNT ON;\s*)?SELECT\b') { throw 'Solo se permiten consultas SELECT.' }
    $command = $Connection.CreateCommand()
    $command.CommandText = $Sql
    $command.CommandTimeout = 120
    foreach ($key in $Parameters.Keys) { [void]$command.Parameters.AddWithValue($key, $Parameters[$key]) }
    $table = New-Object System.Data.DataTable
    $reader = $command.ExecuteReader()
    try { $table.Load($reader) } finally { $reader.Close() }
    return , $table
}

$connection = New-Object System.Data.SqlClient.SqlConnection $builder.ConnectionString
$connection.Open()
try {
    $server = (Invoke-ReadOnlyQuery $connection "SELECT DB_NAME() AS db, CAST(SERVERPROPERTY('Edition') AS NVARCHAR(128)) AS edition, CAST(SERVERPROPERTY('ProductVersion') AS NVARCHAR(64)) AS version, (SELECT COUNT(*) FROM dbo.CatalogoParametro WHERE grupo=N'OPERACION' AND clave=N'AMBIENTE' AND UPPER(valor) IN (N'PRODUCCION',N'PROD',N'PRODUCTION')) AS productionMarkers").Rows[0]
    if ($server.db -ne $Database) { throw "Base inesperada: $($server.db)" }
    if (-not ([string]$server.edition).StartsWith('Developer')) { throw 'Se requiere SQL Server Developer Edition local.' }
    if ([int]$server.productionMarkers -ne 0) { throw 'La base está marcada como producción; se detiene.' }

    if ($Mode -eq 'Snapshot') {
        $snap = (Invoke-ReadOnlyQuery $connection @"
SET NOCOUNT ON; SELECT
 (SELECT COALESCE(SUM(user_updates),0) FROM sys.dm_db_index_usage_stats WHERE database_id = DB_ID()) AS userUpdates,
 (SELECT CONVERT(VARCHAR(33), MAX(last_user_update), 126) FROM sys.dm_db_index_usage_stats WHERE database_id = DB_ID()) AS lastUserUpdate,
 (SELECT COUNT(*) FROM dbo.Sesion) AS sesiones,
 (SELECT COUNT(*) FROM dbo.Asistencia) AS asistencias,
 (SELECT COUNT(*) FROM dbo.EstudianteGrupo) AS matriculas,
 (SELECT COUNT(*) FROM dbo.Horario) AS horarios,
 (SELECT COUNT(*) FROM dbo.Usuario) AS usuarios,
 (SELECT CHECKSUM_AGG(BINARY_CHECKSUM(*)) FROM dbo.Sesion) AS sesionChecksum,
 (SELECT CHECKSUM_AGG(BINARY_CHECKSUM(*)) FROM dbo.Asistencia) AS asistenciaChecksum,
 (SELECT CHECKSUM_AGG(BINARY_CHECKSUM(*)) FROM dbo.EstudianteGrupo) AS matriculaChecksum,
 (SELECT CHECKSUM_AGG(BINARY_CHECKSUM(*)) FROM dbo.Horario) AS horarioChecksum
"@).Rows[0]
        $result = [ordered]@{ takenAt = (Get-Date).ToUniversalTime().ToString('o') }
        foreach ($column in $snap.Table.Columns) { $result[$column.ColumnName] = [string]$snap[$column.ColumnName] }
        $result | ConvertTo-Json -Compress
        return
    }

    $identities = @()
    foreach ($item in $expected) {
        $user = Invoke-ReadOnlyQuery $connection @"
SET NOCOUNT ON; SELECT CONVERT(CHAR(36), u.id) AS idUsuario, u.estado, u.correoConfirmado, CONVERT(CHAR(36), d.id) AS docenteId,
 (SELECT COUNT(*) FROM dbo.Docente d2 WHERE d2.usuario = u.id) AS docentes
FROM dbo.Usuario u LEFT JOIN dbo.Docente d ON d.usuario = u.id WHERE u.id = @id
"@ @{ '@id' = [guid]$item.IdUsuario }
        $group = Invoke-ReadOnlyQuery $connection @"
SET NOCOUNT ON; SELECT CONVERT(CHAR(36), g.id) AS grupoId, g.nombre, g.cantidadEstudiantes, CONVERT(CHAR(36), d.usuario) AS titular,
 (SELECT COUNT(*) FROM dbo.Grupo g2 WHERE g2.nombre = g.nombre) AS homonimos,
 (SELECT COUNT(*) FROM dbo.EstudianteGrupo eg WHERE eg.grupo = g.id) AS matriculas
FROM dbo.Grupo g JOIN dbo.Docente d ON d.id = g.docente WHERE g.id = @gid
"@ @{ '@gid' = [guid]$item.GrupoId }
        $checks = [ordered]@{
            usuarioExiste       = $user.Rows.Count -ge 1
            usuarioActivo       = $user.Rows.Count -ge 1 -and [bool]$user.Rows[0].estado
            unDocente           = $user.Rows.Count -eq 1 -and [int]$user.Rows[0].docentes -eq 1
            grupoExiste         = $group.Rows.Count -eq 1
            grupoNombreUnico    = $group.Rows.Count -eq 1 -and [int]$group.Rows[0].homonimos -eq 1 -and [string]$group.Rows[0].nombre -eq $item.Grupo
            titularDelGrupo     = $group.Rows.Count -eq 1 -and $user.Rows.Count -eq 1 -and ([string]$group.Rows[0].titular).ToLowerInvariant() -eq $item.IdUsuario
        }
        $verified = -not ($checks.Values -contains $false)
        $identity = [ordered]@{
            idUsuario = $item.IdUsuario
            docenteId = if ($user.Rows.Count -eq 1) { ([string]$user.Rows[0].docenteId).ToLowerInvariant() } else { $null }
            grupoId   = $item.GrupoId
            grupo     = $item.Grupo
            checks    = $checks
            verified  = $verified
        }
        if ($verified) {
            $horarios = Invoke-ReadOnlyQuery $connection @"
SET NOCOUNT ON; SELECT LOWER(CONVERT(CHAR(36), h.id)) AS id, LOWER(CONVERT(CHAR(36), h.grupo)) AS grupo
FROM dbo.Horario h JOIN dbo.Grupo g ON g.id = h.grupo JOIN dbo.Docente d ON d.id = g.docente WHERE d.usuario = @id ORDER BY h.id
"@ @{ '@id' = [guid]$item.IdUsuario }
            $sesiones = Invoke-ReadOnlyQuery $connection @"
SET NOCOUNT ON; SELECT LOWER(CONVERT(CHAR(36), s.id)) AS id, s.procedenciaTemporal, CONVERT(VARCHAR(27), s.fechaHoraInicio, 126) AS inicio, CONVERT(VARCHAR(27), s.fechaHoraFin, 126) AS fin
FROM dbo.Sesion s WHERE s.grupo = @gid ORDER BY s.id
"@ @{ '@gid' = [guid]$item.GrupoId }
            $identity['expected'] = [ordered]@{
                horarios   = @($horarios.Rows | ForEach-Object { [ordered]@{ id = $_.id; grupo = $_.grupo } })
                matriculas = [int]$group.Rows[0].matriculas
                sesiones   = @($sesiones.Rows | ForEach-Object { [ordered]@{ id = $_.id; procedenciaTemporal = if ($_.procedenciaTemporal -is [DBNull]) { $null } else { [string]$_.procedenciaTemporal }; inicio = $_.inicio; fin = $_.fin } })
            }
        }
        $identities += $identity
    }

    $evidence = [ordered]@{
        workItem   = 'SEC-BASELINE-002'
        verifiedAt = (Get-Date).ToUniversalTime().ToString('o')
        source     = [ordered]@{ host = $SqlHost; database = [string]$server.db; edition = [string]$server.edition; version = [string]$server.version; access = 'SELECT only (ApplicationIntent=ReadOnly; no read-only principal exists)' }
        identities = $identities
    }
    $dir = Split-Path -Parent $OutFile
    if (-not (Test-Path -LiteralPath $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    $evidence | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $OutFile -Encoding utf8
    foreach ($identity in $identities) {
        $exp = if ($identity.Contains('expected')) { $identity['expected'] } else { $null }
        $summary = if ($null -ne $exp) { "horarios=$(@($exp.horarios).Count) matriculas=$($exp.matriculas) sesiones=$(@($exp.sesiones).Count)" } else { '' }
        Write-Host ("{0} -> {1} [{2}] verified={3} {4}" -f $identity.idUsuario, $identity.grupo, (($identity.checks.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" }) -join ','), $identity.verified, $summary)
    }
    Write-Host "Evidencia: $OutFile"
    if ($identities | Where-Object { -not $_.verified }) { exit 1 }
} finally {
    $connection.Close()
}
