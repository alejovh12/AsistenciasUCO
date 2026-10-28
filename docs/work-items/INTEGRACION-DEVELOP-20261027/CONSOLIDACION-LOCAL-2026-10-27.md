---
status: active
type: validation-report
scope: local-consolidation-and-develop-sync
owner: backend-team
last-reviewed: 2026-10-27
---

# Consolidación local y sincronización de `develop`

Este informe complementa el README del work item y registra únicamente comprobaciones ejecutadas. No cierra el work
item: quedan validaciones manuales, funcionales y contractuales pendientes.

## Resultado ejecutivo

- Backend oficial: `C:\Users\josev\AsistenciasUCO\AsistenciasUCO`.
- Infraestructura oficial activa: `C:\Users\josev\AsistenciasUCO\AsistenciasUCO\infra`.
- No se migró ningún runtime de `AsistenciasUCO-Infra`: sus configuraciones legacy de SQL Server, Keycloak, proxy,
  OTEL y scripts de orchestration usan rutas antiguas, defaults inseguros o una topología distinta de la oficial.
- La carpeta hermana se conserva porque es un repositorio Git con historia y commits exclusivos; no es una segunda
  instalación activa. La eliminación queda pendiente de decisión explícita después de conservar/archivar su historia.
- No se modificó el esquema ni el contenido de la base viva, ni se borraron volúmenes persistentes o respaldos.

## Auditoría de carpetas externas

| Ubicación | Clasificación | Evidencia / acción |
|---|---|---|
| `C:\Users\josev\AsistenciasUCO\AsistenciasUCO-Infra` | RESPALDO / LEGACY | Repositorio Git limpio; rama activa `docs/utc-v2-manual-test-guide-20261025`, 17 commits sobre `origin/main`. Conservado por historia y documentación exclusivas; sus runtimes no se usan. |
| `C:\Users\josev\AsistenciasUCO-P0D-quarantine-2026-10-20` | RESPALDO / CUARENTENA | Conservado; inventario no destructivo: 1.531 archivos y 542 directorios. |
| `C:\Users\josev\AsistenciasUCO-backups-20261027` | RESPALDO / EVIDENCIA | Conservado; inventario no destructivo: 65 archivos y 8 directorios. No se leyeron valores secretos. |
| `C:\Users\josev\AsistenciasUCO-verify-20261027` | TEMPORAL | Worktree limpio después de `verify`; retirado con `git worktree remove`. La rama `codex/fix-hu-guid-mapping-20261027` permanece referenciada en `f8e2c042`. |
| `target`, logs y temporales restantes | NO ELIMINADOS | No se borraron elementos ambiguos ni regenerables fuera del alcance probado. |

El runtime oficial no mantiene referencias a cuarentena, worktrees temporales ni `components/*` legacy. La única
referencia documental al nombre `AsistenciasUCO-Infra` está en `infra/README.md`, como advertencia de que no debe usarse.
El script de paquete A fue corregido para usar el repositorio personal de DB actualizado y el contenedor real
`sql_server_asistencias`; no ejecuta el paquete.

## Docker y preservación de SQL Server

La búsqueda exacta no encontró `asistencias-e2e`; el contenedor real era `asistencias-e2e-sqlserver`, imagen SQL Server
2022, puerto `127.0.0.1:14333->1433`, con la etiqueta `asistencias.e2e.disposable=true`, sin montajes ni volúmenes.
Su base `gestionasistenciadb_e2e` tenía la marca `uco.e2e.disposable=true`. La instancia principal
`sql_server_asistencias` quedó ejecutándose en `0.0.0.0:1433` con el volumen persistente
`sql_server_asistencias_data`; no se ejecutó `down -v`, prune ni eliminación de volúmenes.

Se detuvo y eliminó únicamente `asistencias-e2e-sqlserver`. Después, la base principal continuó accesible y sus conteos
de control permanecieron disponibles: `Usuario=5013`, `Estudiante=5003`, `Asistencia=6354`,
`DetalleAsistencia=6354`, `EstudianteGrupo=6302`, `EstudiantePrograma=5000`, `Sesion=16`.
No se ejecutó el paquete A sobre la base viva ni se restauró, resembró o reemplazó `gestionasistenciadb`.

## Referencias y ramas sincronizadas

Se hizo `fetch --no-tags` de los tres remotos personales; no se hizo push, merge remoto ni force-push.

| Repositorio | `develop` actualizado | Rama local integrada | HEAD local |
|---|---|---|---|
| Backend | `origin/develop` = `e6abbbd2` | `codex/fix-hu-guid-mapping-20261027` | `f8e2c042` |
| Backend | `origin/develop` = `e6abbbd2` | `codex/docs-integracion-develop-20261027` | `9da1ba39` |
| Frontend | `legacy-frontend/develop` = `b90472e3` | `codex/fix-hallazgos-frontend-20261027` | `7cad85d3` |
| Frontend | `legacy-frontend/develop` = `b90472e3` | `codex/integracion-develop-20261027` | `f6753c84` |
| DB | `legacy-db/develop` = `48aafef1` | `codex/fix-grants-runtime-20261027` | `1098d800` |

El merge de DB conservó en `test_suite.sql` la suite de grants y las cuatro suites nuevas de consultas/HU008. El
checkout original de DB fue restaurado a `feat/security-baseline-017-identidad-externa` con su SQL no versionado intacto.
Las cinco ramas de corrección siguen conservadas; no hay autorización para eliminar ninguna.

## Validaciones ejecutadas

| Área | Resultado | Evidencia |
|---|---|---|
| Backend | PASS | Java 25; `mvnw -B -ntp verify` en `f8e2c042`: 2.643 tests, 0 fallos/errores/skips, ArchUnit incluido, JaCoCo `check` cumplido, build exitoso. |
| Frontend correcciones | PASS | `npm run verify` en `7cad85d3`: 1.295/1.295, cobertura líneas 91,84 %, ramas 74,72 %, build producción exitoso. |
| Frontend integración | PASS | `npm run verify` en `f6753c84`: 1.277/1.277, cobertura líneas 91,20 %, ramas 74,35 %, build producción exitoso. |
| Catálogo de seguridad | PASS | `provision_asistencias.py catalog`: 157 endpoints, 0 sin regla, 0 obsoletos. |
| Tests Python del baseline | PASS | 60/60; la primera ejecución en sandbox dio errores de permisos temporales y fue repetida fuera del sandbox. |
| PDP | PASS | `pdp` incremental: 3 recursos agregados, 5 concesiones agregadas, 0 revocadas; `verify` posterior `clean=true`. No se ejecutó `users` ni `--prune`. |
| Decisiones reales PEP→PDP | NOT_RUN | `verify-decisions` no entregó resultado dentro del tiempo operativo y fue cancelado; no se declara PASS. |
| E2E HTTP | NOT_RUN en esta pasada | La evidencia previa queda en backups; no se recreó un SQL desechable después de retirarlo. |
| IntelliJ IDEA | NOT_RUN | `.idea` existe, no hay listener en el puerto 8080 y Maven/JDK fueron validados; no se ejecutó IntelliJ por interfaz. |
| DB viva / paquete A | NOT_RUN | Preservación comprobada por inventario/controles; aplicación aditiva requiere que el usuario ejecute el runbook con `sa`. |

## Pendientes que bloquean el cierre

1. Ejecutar la matriz real PEP→PDP con renovación de token o un timeout controlado y registrar ALLOW/DENY.
2. Ejecutar la validación manual desde IntelliJ y la validación visual/navegador del frontend.
3. Resolver las decisiones abiertas `INT-HU-01`, `DAT-07`, `DAT-08`, `ADM-17` y `DEC-ADM-01` según sus owners.
4. Aplicar el paquete A a la base viva únicamente mediante `live-db/aplicar-paquete-a.ps1`, con respaldo verificable y
   autorización del usuario; el agente no lo ejecutó.
5. En la primera consolidación no se habían preparado PRs; el estado posterior de publicación y checks está en el
   addendum de esta misma evidencia.

## Addendum de publicación y PRs

Actualizado después de publicar las ramas con `git push` normal, sin `--force`:

| Repositorio | Rama publicada / SHA | PR | Estado observado |
|---|---|---|---|
| Backend | `codex/fix-hu-guid-mapping-20261027` / `f8e2c042` | [#53](https://github.com/alejovh12/AsistenciasUCO/pull/53) | Draft, abierto, mergeable; checks en ejecución |
| Backend | `codex/docs-integracion-develop-20261027` / `a378a8e7` | [#54](https://github.com/alejovh12/AsistenciasUCO/pull/54) | Draft, abierto, mergeable; checks en ejecución |
| Frontend | `codex/fix-hallazgos-frontend-20261027` / `7cad85d3` | Pendiente | Push realizado; creación bloqueada por `403 Resource not accessible by integration` |
| Frontend | `codex/integracion-develop-20261027` / `f6753c84` | Pendiente | Push realizado; creación bloqueada por `403 Resource not accessible by integration` |
| DB | `codex/fix-grants-runtime-20261027` / `1098d800` | Pendiente | Push realizado; creación bloqueada por `403 Resource not accessible by integration` |

El conector pudo actualizar los PR Backend, pero no tiene permiso para abrir PRs en `shesho22/AsistenciasUCO-Frontend`
ni `johnjduque/gestion-asistencia-db`. La sesión web tampoco pudo determinar la URL con seguridad y se detuvo; no se
intentó introducir credenciales ni sortear ese control. Esos tres PRs requieren apertura desde una cuenta/CLI con permisos
de los respectivos repositorios.

Checks observados en Backend al momento del corte: `Dependency Review` y `Trivy Repository Vulnerability and
Misconfiguration Scan` en `success`; `CodeQL Java Analysis`, `Backend Quality Gate`, `ArchUnit Architectural Guardrails`
y `Trivy Production Docker Image Scan` en `in_progress`. El check agregado `Trivy` quedó `neutral` porque el workflow
`security-deep-scan.yml`/`trivy-production-image` no está presente en `develop`; no se interpretó como PASS.

No se repitieron las suites completas locales porque no cambió código después de la validación anterior; el nuevo commit
de este addendum es documental. El archivo local previo
`docs/work-items/LOCAL-STABILIZATION-LB004D-2026-10-20/VALIDATION.md` permanece sin incluir en el commit.
