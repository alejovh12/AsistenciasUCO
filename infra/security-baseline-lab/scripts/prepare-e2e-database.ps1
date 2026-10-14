<#
.SYNOPSIS
    SEC-BASELINE-003 - copia consistente de gestionasistenciadb al SQL Server E2E AISLADO y logins de
    mínimo privilegio. Nunca usa sa para el backend.

.DESCRIPTION
    Principal (127.0.0.1:1433, contenedor sql_server_asistencias):
      * huella SELECT antes y después (conteo + CHECKSUM_AGG por tabla, objetos por tipo);
      * UNA sentencia no SELECT: BACKUP DATABASE ... WITH COPY_ONLY, CHECKSUM, COMPRESSION en
        /var/opt/mssql/backup (convención ya usada en el proyecto). COPY_ONLY no altera la cadena de
        log ni las bases diferenciales; solo añade historial en msdb y el archivo .bak.
      * NUNCA DROP/TRUNCATE/ALTER/RESTORE/CREATE LOGIN en el principal.
    E2E (127.0.0.1:14333, contenedor asistencias-e2e-sqlserver):
      * RESTORE VERIFYONLY WITH CHECKSUM + RESTORE como gestionasistenciadb_e2e;
      * marca de aislamiento (extended property) que exigen los scripts de prueba;
      * huella idéntica a la del principal (si difiere, se detiene);
      * login asistencias_e2e_runtime -> rol EXISTENTE rol_asistencias_runtime (definido por el owner DB);
      * login asistencias_e2e_readonly -> rol nuevo rol_asistencias_e2e_lectura = SOLO los SELECT sobre
        vistas que ya concede rol_asistencias_runtime (sin EXECUTE, sin INSERT).
    Escribe %USERPROFILE%\.asistencias-security-lab\e2e-sql-runtime.env y e2e-sql-readonly.env
    (SPRING_DATASOURCE_*) para start-backend-pep-lab.ps1 -DatasourceEnv. Evidencia sin secretos en
    -EvidenceFile.
#>
[CmdletBinding()]
param(
    [string]$BackendEnv = (Join-Path $env:USERPROFILE 'AsistenciasUCO\AsistenciasUCO\.env'),
    [string]$SourceDatabase = 'gestionasistenciadb',
    [string]$EvidenceFile = (Join-Path $env:USERPROFILE '.asistencias-security-lab\e2e-sql-preparation.json'),
    [switch]$ReplaceE2eDatabase
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'LabCommon.psm1') -Force
Import-Module (Join-Path $PSScriptRoot 'E2eSql.psm1') -Force
$s = Get-E2eSqlSettings
$secrets = Import-KeyValueFile $s.SecretsPath
$backend = Import-KeyValueFile $BackendEnv
$evidence = [ordered]@{ workItem = 'SEC-BASELINE-003'; startedAt = (Get-Date).ToUniversalTime().ToString('o') }

$fingerprintSql = @"
SET NOCOUNT ON;
DECLARE @r TABLE (tbl sysname, rows_ bigint, chk int);
DECLARE @t sysname, @q nvarchar(max);
DECLARE c CURSOR LOCAL FAST_FORWARD FOR SELECT QUOTENAME(s.name)+'.'+QUOTENAME(t.name) FROM sys.tables t JOIN sys.schemas s ON s.schema_id=t.schema_id WHERE t.is_ms_shipped=0;
OPEN c; FETCH NEXT FROM c INTO @t;
WHILE @@FETCH_STATUS=0 BEGIN
  SET @q=N'SELECT @n=COUNT_BIG(*), @k=CHECKSUM_AGG(BINARY_CHECKSUM(*)) FROM '+@t;
  DECLARE @n bigint, @k int; EXEC sp_executesql @q, N'@n bigint OUTPUT, @k int OUTPUT', @n OUTPUT, @k OUTPUT;
  INSERT @r VALUES (@t, @n, @k); FETCH NEXT FROM c INTO @t;
END; CLOSE c; DEALLOCATE c;
SELECT 'table' kind, CAST(tbl AS nvarchar(256)) COLLATE DATABASE_DEFAULT name, rows_ n, chk FROM @r
UNION ALL SELECT 'objects', CAST(type_desc AS nvarchar(256)) COLLATE DATABASE_DEFAULT, COUNT(*), CHECKSUM_AGG(CHECKSUM(name)) FROM sys.objects WHERE is_ms_shipped=0 GROUP BY type_desc
UNION ALL SELECT 'modules', CAST(N'definitions' AS nvarchar(256)) COLLATE DATABASE_DEFAULT, COUNT(*), CHECKSUM_AGG(CHECKSUM(definition)) FROM sys.sql_modules
ORDER BY 1,2;
"@
# La huella usa una variable de tabla y sp_executesql de SELECT: no escribe en la base.
function Get-Fingerprint([System.Data.SqlClient.SqlConnection]$Connection) {
    $rows = Invoke-E2eSql $Connection $fingerprintSql
    return ($rows | ForEach-Object { "$($_.kind)|$($_.name)|$($_.n)|$($_.chk)" }) -join "`n"
}

# ---------------------------------------------------------------- principal (lectura + BACKUP COPY_ONLY)
$p = New-E2eConnection -Target Principal -Database $SourceDatabase -User $backend['SPRING_DATASOURCE_USERNAME'] `
    -Password $backend['SPRING_DATASOURCE_PASSWORD'] -AppName 'SEC-BASELINE-003-backup-copy-only'
try {
    if ($p.DataSource -ne $s.PrincipalHost) { throw 'Origen inesperado' }
    $before = Get-Fingerprint $p
    $stamp = (Get-Date).ToUniversalTime().ToString('yyyyMMdd_HHmmss')
    $bakName = "sec_baseline_003_e2e_$stamp.bak"
    $bakPath = "/var/opt/mssql/backup/$bakName"
    Invoke-E2eSql $p "BACKUP DATABASE [$SourceDatabase] TO DISK = @path WITH COPY_ONLY, CHECKSUM, COMPRESSION, NAME = N'SEC-BASELINE-003 E2E isolated copy (COPY_ONLY)', STATS = 25" `
        @{ '@path' = $bakPath } -NonQuery | Out-Null
    $after = Get-Fingerprint $p
    if ($before -ne $after) { throw 'La base principal cambió durante el backup; repetir en un momento sin escrituras.' }
    $history = (Invoke-E2eSql $p "SELECT TOP 1 is_copy_only, has_backup_checksums, CAST(compressed_backup_size AS bigint) size_ FROM msdb.dbo.backupset b JOIN msdb.dbo.backupmediafamily m ON m.media_set_id=b.media_set_id WHERE m.physical_device_name=@path ORDER BY backup_finish_date DESC" @{ '@path' = $bakPath }).Rows[0]
    if (-not $history.is_copy_only -or -not $history.has_backup_checksums) { throw 'El backup no quedó COPY_ONLY + CHECKSUM' }
    $evidence.principal = [ordered]@{ host = $s.PrincipalHost; database = $SourceDatabase; backupFile = $bakPath
        copyOnly = [bool]$history.is_copy_only; checksums = [bool]$history.has_backup_checksums
        compressedBytes = [int64]$history.size_; fingerprintUnchangedDuringBackup = $true }
} finally { $p.Close() }

# ---------------------------------------------------------------- transferencia (fuera de Git)
$localDir = Join-Path (Get-LabSecretsDirectory) 'e2e-sql-backups'
New-Item -ItemType Directory -Force -Path $localDir | Out-Null
$localBak = Join-Path $localDir $bakName
docker cp "$($s.PrincipalContainer):$bakPath" $localBak | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'docker cp desde el principal falló' }
docker exec -u 0 $s.Container mkdir -p /var/opt/mssql/backup | Out-Null
docker cp $localBak "$($s.Container):/var/opt/mssql/backup/$bakName" | Out-Null
docker exec -u 0 $s.Container chown mssql /var/opt/mssql/backup/$bakName | Out-Null
$evidence.transfer = [ordered]@{ localCopy = $localBak; sha256 = (Get-FileHash -Algorithm SHA256 $localBak).Hash
    bytes = (Get-Item $localBak).Length }

# ---------------------------------------------------------------- E2E: verificar, restaurar, marcar
$e = New-E2eConnection -Target E2E -User sa -Password $secrets['MSSQL_SA_PASSWORD'] -AppName 'SEC-BASELINE-003-restore'
try {
    if ($e.DataSource -ne $s.Host) { throw 'Destino inesperado' }
    $server = (Invoke-E2eSql $e "SELECT @@SERVERNAME s").Rows[0].s
    if ($server -ne 'asistencias-e2e-sql') { throw "Servidor E2E inesperado: $server" }
    $e2eBak = "/var/opt/mssql/backup/$bakName"
    Invoke-E2eSql $e "RESTORE VERIFYONLY FROM DISK = @path WITH CHECKSUM" @{ '@path' = $e2eBak } -NonQuery | Out-Null
    $exists = (Invoke-E2eSql $e "SELECT COUNT(*) n FROM sys.databases WHERE name=@d" @{ '@d' = $s.Database }).Rows[0].n
    if ($exists -gt 0 -and -not $ReplaceE2eDatabase) { throw "$($s.Database) ya existe en el E2E; use -ReplaceE2eDatabase" }
    $files = Invoke-E2eSql $e "RESTORE FILELISTONLY FROM DISK = @path" @{ '@path' = $e2eBak }
    $moves = ($files | ForEach-Object {
        $ext = if ($_.Type -eq 'L') { 'ldf' } else { 'mdf' }
        "MOVE N'$($_.LogicalName)' TO N'/var/opt/mssql/data/$($s.Database)_$($_.FileId).$ext'" }) -join ', '
    $replace = if ($exists -gt 0) { ', REPLACE' } else { '' }
    Invoke-E2eSql $e "RESTORE DATABASE [$($s.Database)] FROM DISK = @path WITH $moves, CHECKSUM, RECOVERY$replace" `
        @{ '@path' = $e2eBak } -NonQuery | Out-Null
    $e.ChangeDatabase($s.Database)
    Invoke-E2eSql $e "IF NOT EXISTS (SELECT 1 FROM sys.extended_properties WHERE class=0 AND name=@n) EXEC sp_addextendedproperty @name=@n, @value=@v" `
        @{ '@n' = $s.Marker; '@v' = "copia COPY_ONLY $bakName; solo laboratorio" } -NonQuery | Out-Null
    Assert-E2eTarget $e
    $copy = Get-Fingerprint $e
    if ($copy -ne $after) { throw 'La huella de la copia E2E difiere de la del principal' }
    $evidence.e2e = [ordered]@{ host = $s.Host; server = $server; database = $s.Database; verifyOnly = 'OK'
        fingerprintEqualsPrincipal = $true; fingerprintTables = (($copy -split "`n") | Where-Object { $_ -like 'table|*' }).Count }

    # ------------------------------------------------------------ logins de mínimo privilegio (solo E2E)
    $e.ChangeDatabase('master')
    foreach ($login in @(@('asistencias_e2e_runtime', 'E2E_SQL_RUNTIME_PASSWORD'), @('asistencias_e2e_readonly', 'E2E_SQL_READONLY_PASSWORD'))) {
        Invoke-E2eSql $e @"
DECLARE @sql nvarchar(max);
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name=@login)
  SET @sql = N'CREATE LOGIN ' + QUOTENAME(@login) + N' WITH PASSWORD = ' + QUOTENAME(@pwd, '''') + N', CHECK_POLICY = ON, DEFAULT_DATABASE = ' + QUOTENAME(@db);
ELSE
  SET @sql = N'ALTER LOGIN ' + QUOTENAME(@login) + N' WITH PASSWORD = ' + QUOTENAME(@pwd, '''') + N', DEFAULT_DATABASE = ' + QUOTENAME(@db);
EXEC (@sql);
"@ @{ '@login' = $login[0]; '@pwd' = $secrets[$login[1]]; '@db' = $s.Database } -NonQuery | Out-Null
    }
    $e.ChangeDatabase($s.Database)
    Assert-E2eTarget $e
    Invoke-E2eSql $e @"
IF DATABASE_PRINCIPAL_ID('asistencias_e2e_runtime') IS NULL CREATE USER [asistencias_e2e_runtime] FOR LOGIN [asistencias_e2e_runtime];
IF DATABASE_PRINCIPAL_ID('asistencias_e2e_readonly') IS NULL CREATE USER [asistencias_e2e_readonly] FOR LOGIN [asistencias_e2e_readonly];
IF IS_ROLEMEMBER('rol_asistencias_runtime', 'asistencias_e2e_runtime') = 0 ALTER ROLE [rol_asistencias_runtime] ADD MEMBER [asistencias_e2e_runtime];
IF DATABASE_PRINCIPAL_ID('rol_asistencias_e2e_lectura') IS NULL CREATE ROLE [rol_asistencias_e2e_lectura];
DECLARE @g nvarchar(max) = N'';
SELECT @g = @g + N'GRANT SELECT ON ' + QUOTENAME(OBJECT_SCHEMA_NAME(p.major_id)) + N'.' + QUOTENAME(OBJECT_NAME(p.major_id)) + N' TO [rol_asistencias_e2e_lectura];'
FROM sys.database_permissions p JOIN sys.objects o ON o.object_id = p.major_id
WHERE p.grantee_principal_id = DATABASE_PRINCIPAL_ID('rol_asistencias_runtime') AND p.permission_name = 'SELECT'
  AND p.state_desc = 'GRANT' AND o.type = 'V';
EXEC (@g);
IF IS_ROLEMEMBER('rol_asistencias_e2e_lectura', 'asistencias_e2e_readonly') = 0 ALTER ROLE [rol_asistencias_e2e_lectura] ADD MEMBER [asistencias_e2e_readonly];
"@ -NonQuery | Out-Null
    $perm = Invoke-E2eSql $e @"
SELECT pr.name role_, p.permission_name perm, p.state_desc st, COUNT(*) n
FROM sys.database_permissions p JOIN sys.database_principals pr ON pr.principal_id = p.grantee_principal_id
WHERE pr.name IN ('rol_asistencias_runtime','rol_asistencias_e2e_lectura') GROUP BY pr.name, p.permission_name, p.state_desc ORDER BY 1,2,3
"@
    $evidence.permissions = @($perm | ForEach-Object { "$($_.role_) $($_.st) $($_.perm) x$($_.n)" })
    $evidence.logins = [ordered]@{ runtime = 'asistencias_e2e_runtime -> rol_asistencias_runtime (existente)'
        readonly = 'asistencias_e2e_readonly -> rol_asistencias_e2e_lectura (SELECT vistas)'; sa = 'solo administración del contenedor E2E' }
} finally { $e.Close() }

foreach ($kind in @(@('runtime', 'asistencias_e2e_runtime', 'E2E_SQL_RUNTIME_PASSWORD'), @('readonly', 'asistencias_e2e_readonly', 'E2E_SQL_READONLY_PASSWORD'))) {
    $path = Join-Path (Get-LabSecretsDirectory) "e2e-sql-$($kind[0]).env"
    Set-Content -LiteralPath $path -Encoding ascii -Value @(
        "# SEC-BASELINE-003 - datasource E2E aislado ($($kind[0])); nunca en Git",
        "SPRING_DATASOURCE_URL=jdbc:sqlserver://127.0.0.1:14333;databaseName=$($s.Database);encrypt=true;trustServerCertificate=true;applicationName=asistencias-e2e-$($kind[0])",
        "SPRING_DATASOURCE_USERNAME=$($kind[1])",
        "SPRING_DATASOURCE_PASSWORD=$($secrets[$kind[2]])")
    Protect-LabFile $path
}
$evidence.finishedAt = (Get-Date).ToUniversalTime().ToString('o')
$evidence | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $EvidenceFile -Encoding utf8
$evidence | ConvertTo-Json -Depth 5
