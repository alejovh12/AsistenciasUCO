# Aislamiento SQL Server — SEC-BASELINE-003

Estado: **DB E2E realmente aislada** (por instancia, credenciales y privilegios), con guard automático antes de
cualquier escritura. Resuelve el bloqueo "aislamiento solo operacional / backend con `sa`" de SEC-BASELINE-002.

## Inventario previo (solo `SELECT`, 2026-10-14)

| Elemento | Principal |
|---|---|
| Contenedor / imagen | `sql_server_asistencias`, `mcr.microsoft.com/mssql/server:2022-latest` (`sha256:ba4c8329…`), red `gestion-asistencia-db_default`, volumen `sql_server_asistencias_data`, `0.0.0.0:1433` |
| Versión | SQL Server 2022 RTM-CU26 16.0.4265.3 Developer |
| Bases | `gestionasistenciadb` 208 MB, recovery FULL, compat 160 |
| Esquema | 36 tablas, 56 vistas, 65 SP, 15 funciones escalares, 1 trigger |
| Logins | `sa` (el backend estable lo usa — hallazgo) + grupos Windows del contenedor |
| Roles DB | `rol_asistencias_runtime` (SELECT vistas, EXECUTE SP públicos, INSERT `AuditoriaEvento`, DENY DML directo en `Sesion`/`Usuario`/`PlanEstudio`, DENY SP internos) y `rol_asistencias_clasificador_temporal` — definidos por el owner DB, sin miembros |
| Backups previos | 7 (`COPY_ONLY` + `CHECKSUM` en `/var/opt/mssql/backup`, convención del proyecto) |
| Datos | 5000 estudiantes + 3 docentes `fixture.invalid` (FIX-PAG), 1 `test.local`, 6 semilla con dominio `uco.edu.co`, **1 con dominio `uco.net.co`** (ver "Datos sensibles") |

## Instancia E2E

| Elemento | Valor | Exclusivo |
|---|---|---|
| Contenedor | `asistencias-e2e-sqlserver` (hostname `asistencias-e2e-sql`, label `sec-baseline=003`, `--restart no`) | sí |
| Imagen | la misma imagen local del principal (sin `pull`; el script lo comprueba) | — |
| Puerto | `127.0.0.1:14333` (solo loopback) | sí |
| Volumen | `asistencias-e2e-sqlserver-data` | sí |
| Red | `asistencias-e2e-sql-net` | sí |
| Base | `gestionasistenciadb_e2e` + marca `sec_baseline_003_isolated_e2e` (extended property) | sí |
| Secretos | `%USERPROFILE%\.asistencias-security-lab\e2e-sql.env` (SA y logins; ACL solo usuario) y `e2e-sql-container.env` (solo variables del motor); `e2e-sql-runtime.env` / `e2e-sql-readonly.env` (datasource) | fuera de Git |

Scripts: [`new-e2e-sqlserver.ps1`](../../../infra/security-baseline-lab/scripts/new-e2e-sqlserver.ps1),
[`prepare-e2e-database.ps1`](../../../infra/security-baseline-lab/scripts/prepare-e2e-database.ps1),
[`E2eSql.psm1`](../../../infra/security-baseline-lab/scripts/E2eSql.psm1).

## Copia consistente

1. Huella del principal (conteo + `CHECKSUM_AGG(BINARY_CHECKSUM(*))` de las 36 tablas, objetos por tipo y
   `CHECKSUM_AGG` de definiciones de módulos) — solo lectura.
2. **Única sentencia no `SELECT` en el principal:** `BACKUP DATABASE [gestionasistenciadb] TO DISK =
   '/var/opt/mssql/backup/sec_baseline_003_e2e_20261014_141211.bak' WITH COPY_ONLY, CHECKSUM, COMPRESSION`.
   `COPY_ONLY` no altera la cadena de log ni la base diferencial; efectos: el archivo `.bak` (3,3 MB) en el volumen del
   principal y una fila de historial en `msdb` (backups 7 → 8). No hubo `DROP`, `TRUNCATE`, `ALTER`, `RESTORE` ni
   `CREATE LOGIN` en el principal.
3. Huella posterior idéntica a la previa (sin escrituras concurrentes durante el backup).
4. `docker cp` principal → `%USERPROFILE%\.asistencias-security-lab\e2e-sql-backups\` (SHA-256
   `E779F3A30A0AB252413B93B137A195B5810A831EFC72DEFBD6958BEC11EA238B`) → contenedor E2E.
5. E2E: `RESTORE VERIFYONLY … WITH CHECKSUM` OK; `RESTORE DATABASE [gestionasistenciadb_e2e] … WITH MOVE, CHECKSUM`.
6. Huella de la copia **idéntica** a la del principal (36 tablas + objetos + módulos). Evidencia:
   [evidence/e2e-sql-preparation.json](evidence/e2e-sql-preparation.json).

Fixture FIX-PAG presente en la copia: FIX-PAG-300/1000/5000 con 300/1000/5000 matrículas (FIX-01).

## Principales de mínimo privilegio (solo en la instancia E2E)

| Login | Rol | Permisos efectivos |
|---|---|---|
| `asistencias_e2e_runtime` | `rol_asistencias_runtime` (existente, del owner DB) | 57 SELECT (vistas), 21 EXECUTE (SP públicos), 1 INSERT (`AuditoriaEvento`); DENY explícitos heredados |
| `asistencias_e2e_readonly` | `rol_asistencias_e2e_lectura` (nuevo) | 56 SELECT sobre las **mismas vistas** que concede el rol runtime + `VIEW DEFINITION` sobre `usp_crear_sesion_v2` y `usp_actualizar_sesion_v2` (el backend comprueba su `OBJECT_ID` al arrancar); sin EXECUTE, sin DML |
| `sa` (E2E) | — | solo administración del contenedor E2E (restore, logins, verificación); **nunca** usado por el backend |

`start-backend-pep-lab.ps1 -DatasourceEnv` rechaza un datasource hacia `localhost/127.0.0.1:1433` y el login `sa`.

### Prueba de denegación (`test-e2e-sql-permissions.ps1`, 22/22 PASS)

Cada intento va en `BEGIN TRAN … ROLLBACK` y con `WHERE 1 = 0` cuando aplica (aunque un permiso estuviera mal
concedido, no cambiaría filas). Solo contra la instancia E2E con la marca de aislamiento.

| Login | Permitido | Denegado (error) |
|---|---|---|
| readonly | SELECT `uv_grupo`; `OBJECT_ID` de SP v2 | SELECT tabla base (229), INSERT `AuditoriaEvento` (229), UPDATE `Usuario` (229), DELETE `Asistencia` (229), EXEC `usp_crear_sesion`/`usp_registrar_asistencias_sesion`/`usp_crear_sesion_v2` (229), CREATE TABLE (262) |
| runtime | SELECT `uv_sesion_v2` | UPDATE `Usuario` (229, DENY), INSERT/DELETE `Sesion` (229, DENY), UPDATE `Asistencia` (229), EXEC `usp_crear_sesion_interno` (229, DENY), SELECT tabla base (229), CREATE TABLE (262) |

`ISO-01`: los logins `asistencias_e2e_*` **no existen** en el principal.

### Extremo a extremo

- Backend con login runtime: E2E 25/25 y matriz académica con escrituras (sesiones, asistencias, reclamos, soportes).
- Backend con login readonly: E2E 25/25 (lecturas); `POST /api/v2/sesiones` → **500 `DATABASE_OPERATION_ERROR`** y
  0 filas creadas (SQL Server deniega EXECUTE). Primera tentativa sin `VIEW DEFINITION`: el backend **no arrancó**
  (`SesionV2SchemaCompatibilityVerifier` fail-fast) — comportamiento correcto; se concedió solo `VIEW DEFINITION`.

## Validación de que el backend apunta a la base aislada

[`assert-backend-isolated.ps1`](../../../infra/security-baseline-lab/scripts/assert-backend-isolated.ps1) correlaciona el
PID del proceso que escucha en 8083 con `sys.dm_exec_sessions.host_process_id` (el driver JDBC lo envía):

```json
{"isolated":true,"pid":32940,"port":8083,"e2e":["asistencias_e2e_runtime@gestionasistenciadb_e2e x10"],"principalSessionsOfPid":0}
{"isolated":true,"pid":18668,"port":8083,"e2e":["asistencias_e2e_readonly@gestionasistenciadb_e2e x10"],"principalSessionsOfPid":0}
```

`role_matrix_e2e.py run|readonly-probe` lo ejecuta antes de cualquier escritura y se detiene si no es `isolated:true`.

## Principal después de todo el trabajo

Snapshot de `verify-academic-identity.ps1 -Mode Snapshot` antes (14:02Z), tras la fase 2 (14:08Z), tras la fase 4 (14:38Z) y al final (14:51Z):
`userUpdates=0`, `sesiones=16`, `asistencias=6350`, `matriculas=6302`, `horarios=3`, `usuarios=5011` y checksums
`1820493127 / -1201892611 / 1435088 / 1008836604` **idénticos**. Ninguna sesión del backend experimental en el principal.

## Datos sensibles

La copia **no es 100 % sintética**: además del fixture `fixture.invalid`, contiene 6 usuarios semilla con dominio
institucional y 1 usuario con dominio `uco.net.co` (probablemente una cuenta real de desarrollo; no se imprimió ni se
usó). Controles: instancia solo en loopback, `.bak` en directorio con ACL del usuario, nada versionado. Para un entorno
compartido o CI hace falta un fixture reproducible 100 % sintético o seudonimización (deuda SB3-22).

## Backup, restauración y rollback

| Acción | Comando |
|---|---|
| Recrear la copia desde el principal | `.\infra\security-baseline-lab\scripts\prepare-e2e-database.ps1 -ReplaceE2eDatabase` (nuevo `COPY_ONLY`; solo `REPLACE` en la instancia E2E) |
| Restaurar el `.bak` existente sin tocar el principal | copiar `%USERPROFILE%\.asistencias-security-lab\e2e-sql-backups\sec_baseline_003_e2e_20261014_141211.bak` al contenedor E2E y `RESTORE DATABASE … WITH MOVE, REPLACE` como `sa` E2E |
| Detener el aislado | `docker stop asistencias-e2e-sqlserver` |
| Retirada completa (requiere aprobación: borra datos) | `docker rm asistencias-e2e-sqlserver; docker volume rm asistencias-e2e-sqlserver-data; docker network rm asistencias-e2e-sql-net` |
| Backend vuelve a la DB por defecto | arrancar sin `-DatasourceEnv` (o no arrancar 8083) |
| Principal | nada que revertir; opcionalmente el usuario puede borrar el `.bak` `sec_baseline_003_e2e_*.bak` de `/var/opt/mssql/backup` (no se borró: la instrucción excluye borrar datos) |
