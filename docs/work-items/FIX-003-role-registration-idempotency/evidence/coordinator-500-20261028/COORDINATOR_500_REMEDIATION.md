# Corrección controlada de 500 del módulo Coordinador

Fecha: 2026-10-28 (America/Bogota)

## Alcance y causa confirmada

Se trabajó sobre la única base `gestionasistenciadb` del contenedor `sql_server_asistencias`. Loki devolvió para los tres correlation ID el mismo fallo técnico de SQL Server: `ErrorCode 2812`, `SQLState S0062`, `Could not find stored procedure`.

| Ruta | Correlation ID | Causa confirmada |
|---|---|---|
| `/api/v1/coordinador/docentes/resumen` | `ea8c417a-b63d-40e0-a0ad-ffac5902b9ac` | Faltaba `dbo.usp_consultar_resumen_docentes_programa` |
| `/api/v1/coordinador/indicadores` | `4677165d-70eb-4973-879b-fea61dbec67d` | Faltaba `dbo.usp_consultar_kpis_coordinador` |
| `/api/v1/coordinador/alertas-tempranas` | `4c762ca5-fa93-468b-93a9-0d65020fa622` | Faltaba `dbo.usp_consultar_alerta_temprana_programa` |

No fue un error de permisos: antes de la migración los tres objetos no existían, y `HAS_PERMS_BY_NAME` para el runtime era 0 por ausencia del objeto.

## Evidencia DBA

- Servidor: `2f41e9118416`; base: `gestionasistenciadb`; estado `ONLINE`; recuperación `FULL`; compatibilidad `160`.
- `uco_dev_runtime` pertenece a `rol_asistencias_runtime`.
- La identidad institucional de Jorge tiene una única fila de usuario, una única fila de coordinador activo y una única asignación activa al programa Ingeniería de Sistemas. No se recreó ni modificó esa identidad.
- Las dependencias directas revisadas estaban presentes: `uv_coordinador_identidad`, `uv_programa`, `uv_grupo_programa`, `uv_periodo_academico`, `uv_docente_programa`, `uv_resumen_asistencia_estudiante_grupo`, `uv_cumplimiento_sesion_grupo`, `uv_estudiante_identidad`, `uv_usuario`, `TipoVinculacion`, `ufn_obtener_parametro`, `ufn_obtener_parametro_guid`, `ufn_clasificar_permanencia`, `usp_validar_id_correlacion_esta_presente_interno` y `usp_obtener_mensaje_catalogo`.

Los cinco scripts fuente usados (`usp_resolver_programa_coordinador_interno`, `usp_resolver_periodo_kpi_interno` y los tres SP públicos) tienen el mismo blob SHA de Git en `develop` y `legacy-db/develop`.

## Respaldo

- Archivo externo: `gestionasistenciadb_pre_coordinator_500_20261028_095851.bak`.
- Ubicación: esta carpeta de evidencia.
- Opciones: `COPY_ONLY, CHECKSUM`.
- `RESTORE VERIFYONLY ... WITH CHECKSUM`: correcto; el backup set fue declarado válido.
- Tamaño: `26341376` bytes.
- SHA-256: `F2859B73435B1626B1D5FD310713F799F5950C1E2C3058AEC63780395168D3B6`.

## SQL aplicado

Se ejecutaron únicamente los cinco scripts fuente faltantes, en este orden:

1. `schema/stored-procedures/usp_resolver_programa_coordinador_interno.sql`
2. `schema/stored-procedures/usp_resolver_periodo_kpi_interno.sql`
3. `schema/stored-procedures/usp_consultar_resumen_docentes_programa.sql`
4. `schema/stored-procedures/usp_consultar_kpis_coordinador.sql`
5. `schema/stored-procedures/usp_consultar_alerta_temprana_programa.sql`

Después se aplicaron exactamente estas tres concesiones:

```sql
GRANT EXECUTE ON OBJECT::dbo.usp_consultar_resumen_docentes_programa TO rol_asistencias_runtime;
GRANT EXECUTE ON OBJECT::dbo.usp_consultar_kpis_coordinador TO rol_asistencias_runtime;
GRANT EXECUTE ON OBJECT::dbo.usp_consultar_alerta_temprana_programa TO rol_asistencias_runtime;
```

No se ejecutaron `deploy_schema.ps1`, seeds, `ALTER TABLE`, DML, restauraciones sobre la base principal, grants de esquema, cambios de Keycloak ni cambios de PDP. Los archivos SQL temporales del contenedor fueron eliminados. El archivo local no versionado `rol_asistencias_runtime_gestion_institucional.sql` no se sobrescribió ni ejecutó.

## Verificación posterior

Los cinco objetos existen como `SQL_STORED_PROCEDURE`; los tres públicos tienen `EXECUTE` efectivo para `uco_dev_runtime` mediante `rol_asistencias_runtime`.

Pruebas SQL de solo lectura como runtime con el usuario institucional de Jorge:

- Resumen docente: `4` filas, `4` activos, `0` inactivos.
- KPIs: `1` fila del programa Ingeniería de Sistemas, `4602` estudiantes y `16` sesiones programadas.
- Alertas: `20` filas de la primera página, `4602` registros totales.
- Docentes: `4` filas de `4` totales.
- Grupos: `1` fila de la primera página, `5` totales.
- Sesiones: `1` fila de la primera página, `2` totales.
- Asistencias: `1` fila de la primera página, `2` totales.

Conteos post-migración registrados: `Usuario=5014`, `Coordinador=2`, `Programa=1`, `Docente=5`, `DocentePrograma=4`, `Estudiante=5003`, `EstudiantePrograma=5000`, `EstudianteGrupo=6302`, `Grupo=5`, `Sesion=16`, `Asistencia=6354`, `DetalleAsistencia=6354`, `AuditoriaEvento=271`. La titularidad de Jorge continúa con una asignación de programa.

## Repositorio DB

Se corrigieron sin grants generales:

- `schema/security/rol_asistencias_runtime_inventario_backend.sql`: añadió `usp_consultar_resumen_docentes_programa` y `usp_consultar_historial_grupos_docente_programa` al inventario.
- `schema/security/rol_asistencias_runtime.sql`: añadió la superficie de SP de indicadores, estudiantes y reportes docentes.
- `test/test_runtime_grants_backend_inventory.sql`: alineado con el inventario de 34 procedimientos.

El archivo local no versionado permanece `??` y sin cambios; su SHA-256 actual es `6ED90690DAE014113A45152D769685BDA66581923F0B907B6A6F8903A9C89845`.

## Regresión y pendientes

- Backend dirigido: `BUILD SUCCESS`; 49 pruebas, 0 fallos, 0 errores.
- Backend health: `GET /actuator/health` → `200`, `UP`.
- Frontend: `GET /login` → `200`.
- No se declara `E2E_PASS`: la validación HTTP con la sesión legítima de Jorge quedó `BLOCKED_BY_MISSING_LEGITIMATE_SESSION` porque el helper de navegador no pudo determinar de forma segura la URL actual; no se forzó autenticación ni se usó otra identidad.
- El inventario completo aún identifica como brechas no intervenidas `usp_consultar_bitacora_estudiante_programa`, `usp_consultar_ficha_estudiante_programa`, `usp_consultar_historial_grupos_docente_programa`, `usp_consultar_resumen_asistencia_estudiante_programa` y tres SP de áreas. No se crearon porque exceden la corrección de estos tres 500 y requieren su propia validación/alcance.
- Queda pendiente repetir con la sesión legítima los tres HTTP del Coordinador y la regresión HTTP de Administrador/Decano antes de declarar cierre E2E.
