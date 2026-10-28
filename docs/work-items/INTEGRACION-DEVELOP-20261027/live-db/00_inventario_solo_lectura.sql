/*
  INTEGRACION-DEVELOP-20261027 - Inventario de SOLO LECTURA de la base viva (gestionasistenciadb).
  Requiere un login con metadata visibility completa (p. ej. sa del contenedor). No modifica nada.
  Ejecutar ANTES y DESPUES de aplicar el paquete A y comparar la seccion 3 (conteos) y la 4 (preservacion).
*/
SET NOCOUNT ON;
USE [gestionasistenciadb];

PRINT '== 1. Contexto';
SELECT DB_NAME() AS base, SYSDATETIMEOFFSET() AS momento, CAST(SERVERPROPERTY('ProductVersion') AS varchar(40)) AS version;

PRINT '== 2. Objetos del paquete A (HU051, HU053-HU055)';
SELECT o.nombre,
       CASE WHEN OBJECT_ID('dbo.' + o.nombre, o.tipo) IS NULL THEN 'FALTA' ELSE 'EXISTE' END AS estado,
       o.tipo
FROM (VALUES
  ('SolicitudIngresoGrupo', 'U'),
  ('usp_retirar_estudiante_grupo', 'P'),
  ('usp_consultar_destinatarios_retiro_estudiante_grupo', 'P'),
  ('usp_registrar_solicitud_ingreso_grupo', 'P'),
  ('usp_resolver_solicitud_ingreso_grupo', 'P'),
  ('usp_retirar_solicitud_ingreso_grupo', 'P'),
  ('usp_consultar_solicitudes_ingreso_estudiante', 'P'),
  ('usp_consultar_solicitudes_ingreso_grupo_docente', 'P'),
  ('usp_consultar_destinatarios_solicitud_ingreso_grupo', 'P')
) o(nombre, tipo);

SELECT id, nombre, codigo FROM dbo.Estado ORDER BY codigo;

SELECT 'estado E (en espera)' AS item, COUNT(*) AS filas FROM dbo.Estado WHERE codigo = 'E'
UNION ALL SELECT 'estado X (retirada)', COUNT(*) FROM dbo.Estado WHERE codigo = 'X'
UNION ALL SELECT 'mensaje RET_005', COUNT(*) FROM dbo.CatalogoMensajeUsuario WHERE codigo = 'RET_005'
UNION ALL SELECT 'mensaje SIG_006', COUNT(*) FROM dbo.CatalogoMensajeUsuario WHERE codigo = 'SIG_006';

PRINT '== 2b. Objetos del paquete B (KPIs, catalogos, HU010-HU011): solo informativo, NO forma parte del paquete A';
SELECT o.nombre, CASE WHEN OBJECT_ID('dbo.' + o.nombre, 'P') IS NULL THEN 'FALTA' ELSE 'EXISTE' END AS estado
FROM (VALUES
  ('usp_consultar_kpis_decano'), ('usp_consultar_kpis_coordinador'), ('usp_consultar_kpis_institucion'),
  ('usp_consultar_aforo_grupos_facultad'), ('usp_consultar_comparativa_programas_facultad'),
  ('usp_consultar_comparativa_facultades'), ('usp_consultar_tendencia_ausentismo'),
  ('usp_consultar_alerta_temprana_programa'), ('usp_consultar_ficha_estudiante_programa'),
  ('usp_consultar_bitacora_estudiante_programa'), ('usp_consultar_solicitudes_revision_estudiante'),
  ('usp_crear_area'), ('usp_actualizar_area'), ('usp_cambiar_estado_area')
) o(nombre);

PRINT '== 2c. Permisos del rol runtime sobre el paquete A (EXECUTE)';
SELECT p.name AS procedimiento,
       CASE WHEN EXISTS (SELECT 1 FROM sys.database_permissions dp
                         JOIN sys.database_principals pr ON pr.principal_id = dp.grantee_principal_id
                         WHERE dp.major_id = p.object_id AND dp.permission_name = 'EXECUTE' AND dp.state = 'G'
                           AND pr.name = 'rol_asistencias_runtime') THEN 'CONCEDIDO' ELSE 'SIN_CONCESION' END AS execute_runtime
FROM sys.procedures p
WHERE p.name IN ('usp_retirar_estudiante_grupo','usp_consultar_destinatarios_retiro_estudiante_grupo',
                 'usp_registrar_solicitud_ingreso_grupo','usp_resolver_solicitud_ingreso_grupo',
                 'usp_retirar_solicitud_ingreso_grupo','usp_consultar_solicitudes_ingreso_estudiante',
                 'usp_consultar_solicitudes_ingreso_grupo_docente','usp_consultar_destinatarios_solicitud_ingreso_grupo');

SELECT m.name AS miembro, r.name AS rol
FROM sys.database_role_members rm
JOIN sys.database_principals r ON r.principal_id = rm.role_principal_id
JOIN sys.database_principals m ON m.principal_id = rm.member_principal_id
WHERE r.name = 'rol_asistencias_runtime';

PRINT '== 3. Conteo de filas por tabla (linea base de preservacion)';
SELECT t.name AS tabla, SUM(p.rows) AS filas
FROM sys.tables t
JOIN sys.partitions p ON p.object_id = t.object_id AND p.index_id IN (0, 1)
GROUP BY t.name
ORDER BY t.name;

PRINT '== 4. Preservacion del laboratorio (titularidad y cuentas)';
SELECT f.id AS idFacultad, f.nombre AS facultad, d.id AS idDecano,
       CONCAT(u.primerNombre, ' ', u.primerApellido) AS decanoTitular, u.correo, u.estado AS usuarioActivo
FROM dbo.Facultad f
JOIN dbo.Decano d ON d.id = f.decano
JOIN dbo.Usuario u ON u.id = d.usuario
WHERE f.id = 'A2B3C4D5-0000-0000-0000-000000000001';

SELECT u.id AS idUsuario, u.correo, u.estado
FROM dbo.Usuario u
WHERE u.id = '92C4847C-7AED-40AA-BFF8-34C5F787C473';

SELECT COUNT(*) AS identidadesExternas FROM dbo.IdentidadExterna;
