/*
 * MAINT-003J — SQL Server read-only DEV inventory.
 * Run with sqlcmd -b after confirming correct server/container.
 * NO DDL/DML, no transaction, no credentials here.
 * NOT a deploy/reset script. This script reports schema/fixture state.
 */
SET NOCOUNT ON;
SELECT targetServer = @@SERVERNAME, targetDatabase = DB_NAME(), utcNow = SYSUTCDATETIME();
IF DB_NAME() <> N'gestionasistenciadb'
    THROW 52310, 'DEV_PREFLIGHT: unexpected database', 1;
IF OBJECT_ID(N'dbo.Sesion', N'U') IS NULL
    THROW 52311, 'DEV_PREFLIGHT: missing Sesion', 1;
SELECT
  sesionTotal = COUNT_BIG(*),
  sesionUnknown = SUM(CASE WHEN procedenciaTemporal IS NULL THEN CONVERT(bigint,1) ELSE CONVERT(bigint,0) END),
  sesionConfirmed = SUM(CASE WHEN procedenciaTemporal IS NOT NULL THEN CONVERT(bigint,1) ELSE CONVERT(bigint,0) END)
FROM dbo.Sesion;
SELECT name, type_desc
FROM sys.objects
WHERE schema_id = SCHEMA_ID(N'dbo')
  AND name IN (N'uv_sesion_v2',N'usp_crear_sesion_v2',N'usp_actualizar_sesion_v2',
    N'usp_sincronizar_usuario',N'usp_registrar_o_actualizar_plan_estudio')
ORDER BY name;
SELECT
  referencedTable = OBJECT_SCHEMA_NAME(fk.referenced_object_id) + N'.' + OBJECT_NAME(fk.referenced_object_id),
  childTable = OBJECT_SCHEMA_NAME(fk.parent_object_id) + N'.' + OBJECT_NAME(fk.parent_object_id),
  fkName = fk.name
FROM sys.foreign_keys fk
WHERE fk.referenced_object_id IN (OBJECT_ID(N'dbo.Sesion'),OBJECT_ID(N'dbo.Asistencia'),
  OBJECT_ID(N'dbo.DetalleAsistencia'))
ORDER BY referencedTable,childTable;
SELECT tableName=t.name, approxRows = SUM(CASE WHEN p.index_id IN (0,1) THEN p.rows ELSE 0 END)
FROM sys.tables t
JOIN sys.partitions p ON p.object_id=t.object_id
WHERE t.name IN (N'Usuario',N'Estudiante',N'EstudianteGrupo',N'Grupo',N'Sesion',N'Asistencia',
 N'DetalleAsistencia',N'AuditoriaEvento',N'PlanEstudio')
GROUP BY t.name ORDER BY t.name;
