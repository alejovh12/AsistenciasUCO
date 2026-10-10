---
date: 2026-10-10
status: DEV_ONLY_PREPARED_NOT_EXECUTED
target_database: gestionasistenciadb
target_container: sql_server_asistencias
---
# Runbook — actualización de DB original DEV y limpieza de datos de prueba

**Esta guía autoriza preparar y ejecutar en DEV verificada, no ejecutar a ciegas.** No mezclar los datos de `utc_d06_sqltest`/`cc003g01_sqltest` con la base original. La meta es que la **única DB de desarrollo utilizada por backend** incorpore DB-D06 + CC-003G-01 y los datos de prueba representativos.

## Preflight obligatorio (read-only)
1. Revisar `docker ps --format ...`, volúmenes, redes, puertos y qué contenedor conecta el backend. Esperado `sql_server_asistencias`, DB `gestionasistenciadb`; confirmar `DB_NAME()`, `@@SERVERNAME`, host/puerto y ambientes; FAIL si no corresponden.
2. Confirmar que no hay datos institucionales reales/terceros ni otros consumidores activos y que backend/fixtures están detenidos para ventana de reset.
3. Ejecutar `DEV_DB_PRECHECK.sql` en SOLO LECTURA; almacenar reportes de `Sesion`, `Asistencia`, `DetalleAsistencia`, estudiantes, grupos, usuarios, `sys.foreign_keys`, estado de columna y SP D06/CC-003G-01. Verificar si seed 14 vuelve a introducir sesiones con NULL.
4. Respaldo de DEV **verificado/restaurable**, en ruta fuera del repo y con política de retención; no eliminar un volumen con datos necesarios. Probar restore en contenedor temporal, sin sustituir la DB original.

## Elección de limpieza
**Preferida para DEV desechable**: generar DB desde cero con scripts actuales en el mismo servicio `sql_server_asistencias` (con ventana sin clientes), cuando se haya demostrado que catálogos, roles, seed y fixtures se reconstruyen. Conservar `dbo.AuditoriaEvento` append-only en una DB que se mantiene; una reconstrucción total de DB sólo debe hacerse cuando *todo* el ambiente sea explícitamente desechable y el backup sea restaurable. No usar TRUNCATE CASCADE inexistente en SQL Server, ni DELETE ignorando FKs.

**Alternativa**: limpieza transaccional selectiva de sesiones legacy y dependientes según inventario real de FKs (detalles → asistencias → sesiones, más cualquier referencia adicional). Nunca `UPDATE fechaHoraInicio/Fin` de un histórico desconocido para marcarlo UTC. Registrar conteos antes/después, rollbacks y oráculos; script DEV-only en work item DB nuevo, no en seed general.

### P1 — impedir reaparición de sesiones ambiguas
`schema/seed/14_grupos_sesiones.sql` todavía contiene `MERGE dbo.Sesion` con literales `2026-08-17 08:00`/ `10:00` y `2026-08-24 08:00`/`10:00` **sin procedencia**. Al volver a correr `deploy_schema.ps1` esas filas aparecen de nuevo (estado NULL). Antes del reset, Codex deberá elegir y documentar:
- opción recomendada: retirar **solo** ese bloque MERGE de `Sesion` del seed general, dejando grupos y matriculaciones intactos, y generar sesiones DEV mediante `usp_crear_sesion_v2` con docente/offset auténticos o `usp_generar_sesiones_grupo` con zona institucional;
- no usar `UPDATE procedenciaTemporal='UTC_V2'` sobre literales históricos ni asignar etiqueta falsa desde SQL seed;
- SQL fixture DEV debe comprobar UTC y permitir consultar desde Bogotá/Berlín. El procedimiento SQL `usp_crear_sesion_v2` recibe valores UTC normalizados, **no** un string HTTP con offset.

## Orden de despliegue DEV (sin copiar bases de test)
1. Trabajar con las **ramas exactas** DB D06 `feat/utc-d06-post-freeze` y CC-003G-01 `feat/cc-003g-01-public-user-plan-providers`: la segunda está 4 commits encima de la primera. Tomar el HEAD CC-003G-01 para desplegar **una sola vez**, no ejecutar scripts inconsistentes de ramas diferentes.
2. Probar despliegue de ese HEAD en nuevo contenedor de ensayo limpio, y contra RESTORE de DEV, con idempotencia, SQL test summary y permisos del login runtime. Detener antes de aplicar si difieren schemata/freeze esperado.
3. Modificar/validar seed-14 para no sembrar sesiones legacy; realizar reset DEV escogido y documentado. Ejecutar `deploy_schema.ps1 -ContainerName sql_server_asistencias` tomando contraseña de entorno local y nunca imprimiéndola ni subiéndola a GitHub; el script crea DB si falta, instala tablas→triggers→funciones→vistas→SP→security→seed. El script no hace reset ni backup por sí mismo.
4. Comprobar catálogo, existencia de `uv_sesion_v2`, de `usp_crear_sesion_v2`, `usp_actualizar_sesion_v2`, `usp_sincronizar_usuario`, `usp_registrar_o_actualizar_plan_estudio`, `procedenciaTemporal`, perfiles, roles, permisos/grants efectivos; comprobar hash y cantidad de objetos antes/después.
5. Generar fixtures **realistas** pero sintéticos: usuarios/estudiantes, grupos y matrículas con estados válidos; 300/1000/5000 alumnos según alcance de paginado, diferentes cadenas/acentos/apellidos, estados activos/inactivos, correos y documentos únicos; algunas sesiones confirmadas creadas por v2/generador, asistencias AN/SJC/EX, casos de filtros y orden. Nunca crear 5000 cuentas Keycloak; los fixtures SQL de carga no son identidades autenticables, salvo cuentas manuales mínimas para pruebas JWT. Evitar colisiones con seeds y ejecución destructiva en cada redeploy.
6. Ejecutar SQL gate, Java25 `-Pintegration clean verify`, RBAC 401/403, v2 POST/GET/PATCH/GET, interfaz docente, consultas de paginado y asistencia y v1 estudiante/Excel. Verificar `DateTime2(7)` en 3 zonas de JVM, y `Sesion` sin NULL de procedencia **en los fixtures nuevos**.
7. Levantar backend para DEV con `app.sesiones.v2.enabled=true` **solo cuando verifique la DB desplegada**. Frontend habilita `UTC_SESSIONS_V2_ENABLED=true` solo en DEV tras smoke; rollback flag OFF.
8. Guardar `VALIDATION`: DB host/container exacto, HEAD, scripts, resultados, manifest/recuento de datos sintéticos, test matrix, checksum; no guardar password, JWT ni emails reales.

## Retiro de temporales — después de DEV PASS
`utc_d06_sqltest` y `cc003g01_sqltest` son ambientes efímeros, eliminables tras guardar reports, confirmar que ningún backend apunta allí y que no alojan objetos únicos. Reunir `docker inspect` y `docker volume ls` antes de `docker rm`; **no ejecutar** `docker system prune --volumes` ni borrar `asistencias-minio-data`, `asistencias-clamav-data`, Keycloak, observabilidad ni DB original.

## Rollback
Si falla migración en DEV, flag UTC OFF, detener backend, restaurar respaldo verificado o reconstruir DB desechable de cero desde SHAs anteriores y volver a fixtures compatibles. No ocultar error convirtiendo timestamps legacy en instantes.
