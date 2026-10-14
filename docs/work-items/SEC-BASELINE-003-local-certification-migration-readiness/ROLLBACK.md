# ROLLBACK — SEC-BASELINE-003

Nada de este work item está en `develop`, en ambientes compartidos ni en el frontend. Todo es reversible sin tocar el
Keycloak 8081 ni el SQL principal.

| Elemento | Cómo revertir | Observación |
|---|---|---|
| Backend experimental 8083/8084 | No arrancarlo, o arrancarlo sin `-IdentityMode SharedLab`, sin `-DatasourceEnv` y/o con `-PepDisabled` | El flag PEP sigue OFF por defecto en `application.yml` |
| Merge FIX-001 en la rama experimental (`8b50343`) | `git revert -m 1 8b50343` en la rama experimental | Innecesario: es el mismo código ya fusionado en `develop` |
| Cambios de arnés/scripts (`a53fe25` y el commit de cierre) | `git revert <sha>` en la rama experimental | Solo `infra/security-baseline-lab/**` y documentación |
| Keycloak 19090 (laboratorio) | Deshabilitar o borrar (con aprobación) los usuarios `lab-e2e-administrador`, `-decano`, `-coordinador`, `-docente-seed`, `-estudiante`, `-docente-fixpag1000`, `-docente-fixpag5000`, `-revocable`, `-sin-rol`; quitar los client roles `ADMINISTRADOR`/`DECANO`/`COORDINADOR` de `asistencias-api` y su scope mapping en `asistencias-lab-e2e` | Solo realm `security-baseline` del laboratorio |
| PDP (laboratorio) | Quitar asignaciones y borrar el rol `MATRIZ_ROLES_E2E_LAB` y los recursos exactos creados (lista en `evidence/role-matrix-provisioning.json` + recursos dinámicos `/api/v2/sesiones/{id}`, `/api/v1/sesiones/{id}`, `/api/v1/docente/reclamos/{id}`, `/api/v1/estudiante/reclamos/{id}`, `/api/v1/archivos/{id}`) | Borrar requiere aprobación (instrucción: no borrar datos); deshabilitar asignaciones basta para revertir el efecto |
| PEP / PDP detenidos durante las pruebas | Ya rearrancados (`docker start`); readiness `UP` / `healthy` | — |
| SQL E2E aislado | `docker stop asistencias-e2e-sqlserver`; retirada completa (con aprobación): `docker rm`, `docker volume rm asistencias-e2e-sqlserver-data`, `docker network rm asistencias-e2e-sql-net` | No afecta al principal |
| Secretos locales nuevos | `%USERPROFILE%\.asistencias-security-lab\e2e-sql*.env`, contraseñas `LAB_USER_LAB_E2E_*` en `lab.env`, `e2e-sql-backups\*.bak` | Fuera de Git; borrar solo con aprobación |
| SQL principal | Nada que revertir. Opcional (usuario): borrar `/var/opt/mssql/backup/sec_baseline_003_e2e_20261014_141211.bak` | La fila de historial en `msdb` es inocua |
| Escrituras de la matriz | SQL: solo en `gestionasistenciadb_e2e` (sesiones `SEC003 E2E *`, asistencias, solicitudes de revisión aprobadas). Fuera de SQL: 3 objetos de soporte en el MinIO **local de desarrollo** (no aislado; ver abajo) | Recrear la copia con `prepare-e2e-database.ps1 -ReplaceE2eDatabase`; los objetos MinIO de prueba pueden quedar (bucket local) |

## Soportes en MinIO

`DL-05` subió tres PDF mínimos (uno por cada corrida completa de la matriz) al MinIO local (`minio_asistencias`, usado también por el
backend estable en desarrollo). Son objetos de prueba sin datos personales, propiedad del `idUsuario` del estudiante
semilla. No se borraron (la instrucción excluye borrar datos); el usuario puede eliminarlos desde la consola de MinIO
si lo desea.
