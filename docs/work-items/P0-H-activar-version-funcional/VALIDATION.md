# VALIDATION — P0-H: activar y validar la última versión funcional

## Dictamen

`LOCAL_EXPERIMENTAL_PASS` para la combinación candidateada y el flujo real local. `NOT_READY / CONTRACT_CONFLICT` para elevar D1/R2 a integración oficial: la decisión del owner DB sobre R1/R2 y la autoridad del remoto siguen pendientes.

La validación no cierra el contrato oficial ni modifica producción. No se copiaron secretos al repositorio. El work item permanece abierto para la decisión contractual y la evidencia manual de IntelliJ/UI.

## Comandos ejecutados

| Fecha/entorno/base | Comando | Exit code | Resultado | Evidencia sanitizada |
|---|---|---:|---|---|
| 2026-10-20 / backend actual | inventario de refs/ascendencia Git | 0 | PASS | backend `6893f4d`; candidato `f45a5213`; frontend `95a202a` y `fac2f640`; DB `b698c76` |
| 2026-10-20 / backend actual | snapshot de cambios locales | 0 | PASS | `.p0-h-snapshot-20261020/` y stash `c09a7c4d...` preservados |
| 2026-10-20 / frontend original | `git status`, refs y ascendencia | 0 | PASS | tres archivos locales preservados; `fac2f640` ancestro de `95a202a` |
| 2026-10-20 / backend integration | `.\mvnw.cmd -B -ntp verify` | 0 | PASS | 1641 tests, 0 failures, 0 errors, 0 skipped; build SUCCESS |
| 2026-10-20 / frontend integration | `npm.cmd run verify` | 0 | PASS | 807 SUCCESS; realtime check y production build PASS |
| 2026-10-20 / stack real | `.\scripts\start-local-stack.ps1 -Check` | 0 | PASS | 1433/8081/9000/3310/8080/4200 LISTENING; `infra/files/.env` MISSING |
| 2026-10-20 / E2E real | `support_binding_e2e.py main` con SQLCMD en memoria | 0 | PASS | 34 checks, 34 PASS, 0 FAIL, 0 SQL NOT_RUN |
| 2026-10-20 / E2E sesiones | `student_sessions_e2e.py` con SQLCMD en memoria | 1 | FAIL parcial | 15 checks: 11 PASS, 4 FAIL; Arquitectura de Software y 2 sesiones PASS, estado de asistencia no coincide con el fixture esperado |
| 2026-10-20 / DB local respaldada | fixture LB-004D + concurrencia | 0 | PASS | resultsets, ownership, rollback, mínimo privilegio y concurrencia PASS |

## Checks

| Nivel | Check | PASS / FAIL / NO APLICA / BLOQUEADO | Evidencia/motivo |
|---|---|---|---|
| Alcance | plan y preservación | PASS | snapshots y referencias candidateadas registrados |
| Arquitectura | ArchUnit/dependencias | PASS | incluido en `mvn verify`; 1641 tests pasan |
| Contratos | HTTP/DB/security | PASS local / BLOQUEADO oficial | E2E, metadata y negativos reales pasan; R1/R2 no se elevan a contrato oficial |
| Tests | RED → GREEN | PASS | tests candidateados ejecutados sin rebajarlos ni modificarlos para ocultar fallos |
| Coverage | líneas ≥80 %, ramas ≥70 % | PASS | backend line 91.23 %, branch 80.07 %; frontend line 88.99 %, branch 70.68 % |
| Integración | DB/provider real | PASS local | SQL Server, Keycloak, MinIO y ClamAV reales; no mocks |
| Build | Maven verify / frontend verify | PASS | ambos exit 0 |
| Seguridad | JWT/RBAC/ownership/secret hygiene | PASS local | 34 E2E PASS, negativos 401/403/404, EICAR rechazado; secretos no registrados |
| Runtime | procesos y wiring | PASS | PIDs nuevos desde checkouts originales; backend health 200 y frontend 200 |
| Manual UI | IntelliJ + login visual docente/estudiante | NOT_RUN parcial | helper sin ventana IntelliJ seleccionable; no se inyectaron contraseñas al registro del navegador |
| Docente / sesiones | Arquitectura de Software, grupo y dos sesiones | PASS parcial | E2E de sesiones devuelve materia y exactamente 2 sesiones; no se certifica el estado de asistencia por fixture inconsistente |
| Documentos | work item y trazabilidad | PASS | PLAN/TEST_PLAN/VALIDATION creados |

## Integridad de RED

No aplica RED nuevo. Los candidatos y sus tests se fijan por SHA; cualquier conflicto de contrato se devuelve al owner correspondiente.

## Bloqueantes y limitaciones

- `CONTRACT_CONFLICT: P0H-DB-001`: DB baseline v1 fija resultset público de cuatro columnas, mientras D1/R2 candidatea R1 con diez y R2 con veinticinco columnas de lectura. Requiere decisión del owner DB.
- `BLOCKED_BY_MISSING_EVIDENCE: P0H-INTELLIJ-001`: el helper de escritorio no expuso una ventana/app seleccionable de IntelliJ; el runtime equivalente sí quedó iniciado desde el backend original mediante `spring-boot:run`.
- `DATA_FIXTURE_MISMATCH: P0H-DATA-001`: `student_sessions_e2e.py` encontró detalles de asistencia en ambas sesiones; el contrato del harness exige SES-01 sin detalle y SES-02 `SJC`. No se limpiaron filas históricas ni se inventó un fixture correctivo.
- `infra/files/.env` no está presente en el checkout. Los servicios de archivos ya estaban levantados y sanos; un arranque limpio futuro requiere ese archivo local ignorado aportado por un operador autorizado.
- La evidencia histórica de P0-C/P0-E orienta la reproducción, pero no es resultado actual.
- El snapshot Git se conserva porque el índice del backend no permitió `stash`; la recuperación se hace con el patch/manifest, sin borrar backups.

## Evidencias remotas/manuales pendientes

IntelliJ/UI y decisión contractual D1/R2 siguen pendientes. El resto quedó comprobado: backend `b48ce40f` / PID `19136` / puerto 8080 / health UP; frontend `6bf5787` / PID `11244` / puerto 4200; DB `b698c76`; SQL Server, Keycloak, MinIO y ClamAV activos; no se creó PR publicado.

## Evidencia de integración y runtime

| Componente | Checkout/rama/SHA | Resultado |
|---|---|---|
| Backend | `jose-valencia/p0-h-backend-functional-integration` / `b48ce40f4bf99fbd17a09ad7be9245650f25400e` | proceso PID 19136 iniciado 16:53:59 -05:00; `Started AsistenciasUcoApplication`; Tomcat 8080; health 200 `UP` |
| Frontend | `jose-valencia/p0-h-frontend-functional-integration` / `6bf5787aacbb176d55aa6b99e021df87fb289e93` | `95a202a` funcional y `fac2f640191cab4c6942d2cfed2c18770d0c4378` ancestro; PID 11244 iniciado 16:54:57 -05:00; 4200 responde 200 |
| DB | `jose-valencia/p0-h-db-functional-integration` / `b698c76112490b91298d16ccd1fb6a793648adec` | `gestionasistenciadb` ONLINE; cuatro SP presentes; no se aplicó esquema desde P0-H |
| Proveedores | SQL Server 1433, Keycloak 8081, MinIO 9000, ClamAV 3310 | contenedores activos; MinIO/ClamAV healthy; `start-local-stack.ps1 -Check` PASS |

El frontend usa por defecto `http://<host>:8080/api/v1` en `src/environments/environment.ts`; la ruta docente sin Bearer respondió 401, confirmando el wiring HTTP real.

## DB y E2E real

La DMV de SQL Server confirmó W1=4 columnas, W2=4, R1=10 y R2=25, sin errores de metadata. El backup copy-only del 2026-10-19 pasó `RESTORE VERIFYONLY`; después se ejecutaron el fixture transaccional LB-004D y la concurrencia: ambos exit 0, rollback/limpieza comprobados.

El flujo real comprobado fue: estudiante sube PDF limpio → ClamAV acepta → MinIO guarda → SQL crea `DRAFT` → propietario descarga → estudiante radica con `soporteArchivoId` → SQL vincula `ATTACHED` → docente titular lista y descarga con SHA-256 idéntico. También pasaron negativos de 401/403/404, docente ajeno, otro estudiante del grupo, coordinador, administrador, segunda radicación, EICAR, tamaño >5 MiB, nombre >250 y ejecutable disfrazado.

La primera corrida tuvo 3 SQL `NOT_RUN` por no heredar `SQLCMDPASSWORD`; se repitió con la variable solo en memoria y terminó `34/34 PASS`, `0 FAIL`, `0 SQL NOT_RUN`. Las identidades E2E se reprovisionaron únicamente en Keycloak local y sus secretos permanecen fuera del repo.

El E2E de sesiones fue lectura real: `Arquitectura de Software` y sus dos sesiones se obtuvieron correctamente, pero terminó `11/15 PASS` porque los datos actuales no corresponden al estado de ausencia esperado por el fixture existente. El fallo queda separado del código recuperado; no se ejecutó `apply`/`cleanup` sobre esas filas.

## Estado de cierre

No se crea `CLOSURE.md`: la validación experimental local pasa, pero la Definition of Done de integración oficial no se cumple mientras siga abierto `P0H-DB-001` y no exista evidencia IntelliJ/UI completa. El siguiente paso es el dictamen del owner DB y la auditoría/cierre independiente.

## Preparación contractual

- Backend: PR draft [#31](https://github.com/alejovh12/AsistenciasUCO/pull/31), base `master`, head `jose-valencia/p0-h-backend-functional-integration`, abierto y sin merge.
- Frontend: rama publicada; enlace preparado [crear PR](https://github.com/AsistenciasUCO/AsistenciasUCO-Frontend/pull/new/jose-valencia/p0-h-frontend-functional-integration), base `develop`. El conector GitHub respondió `403 Resource not accessible by integration`, por lo que no se declara PR creado.
- DB: rama publicada; enlace preparado [crear PR contractual](https://github.com/AsistenciasUCO/AsistenciasUCO-DB/pull/new/jose-valencia/p0-h-db-functional-integration), base `develop`. El conector GitHub respondió `403 Resource not accessible by integration`, por lo que no se declara PR creado.
- No se hizo merge, no se hizo force-push y no se modificaron las ramas base.

## Addendum de validación — 2026-10-24

Este addendum registra la corrida actual; las cifras y procesos descritos arriba pertenecen a evidencia histórica del work item y no se reutilizan como resultado actual.

| Checkout | Acción actual | Resultado |
|---|---|---|
| Backend `jose-valencia/p0-h-backend-functional-integration` | fast-forward local desde `origin/develop` a `1b62498c89a7b0d094278b7917b167e7eb5f0033`; backup `codex/p0-h-pre-sync-20261024-backend` | sin conflictos; `git diff --check` PASS |
| Frontend `jose-valencia/p0-h-frontend-functional-integration` | fast-forward local desde `legacy-frontend/develop` a `ecaba7280fc20324e9cb81876b62bc1c7046b6da`; backup `codex/p0-h-pre-sync-20261024-frontend` | sin conflictos; `git diff --check` PASS |
| DB `jose-valencia/p0-h-db-functional-integration` | merge local de `legacy-db/develop` en `26d64eb28467d8ae59f1d53a6c28dd188fb5b005`; backup `codex/p0-h-pre-sync-20261024-db` | sin conflictos; commit P0-H conservado; `git diff --check` PASS |

### Corridas actuales

- Backend `.\mvnw.cmd -B -ntp verify`: exit 0, 2.046 tests, 0 failures, 0 errors, 0 skipped, `BUILD SUCCESS`; JaCoCo línea 91,36 % y rama 81,53 %.
- Frontend `npm.cmd run verify`: exit 0, 1.043 tests exitosos, realtime check PASS y build productivo PASS; lines 90,17 % y branches 72,30 %.
- DB `test_summary.ps1`: `NOT_RUN / BLOCKED_BY_ENVIRONMENT`; el script necesita Docker/SQL Server Developer y password local no versionada. `docker ps --all` devolvió `Access is denied` en el engine.
- E2E conjunto y `-Pintegration verify`: `NOT_RUN`; no hay proveedor externo controlado disponible.

### Dictamen actualizado

La sincronización local es reproducible y reversible, pero el estado correcto sigue siendo `LOCAL_EXPERIMENTAL_PASS / PARCIALMENTE LISTA`. No se autoriza cierre oficial mientras estén pendientes la certificación DB/runtime, E2E, decisión contractual D1/R2 y el detalle de la alerta de seguridad referida por el usuario. El informe completo está en [AUDIT_2026-10-24.md](AUDIT_2026-10-24.md).

## Addendum de continuación — validación real local 2026-10-24

Este addendum conserva los resultados históricos anteriores y registra la ejecución posterior con el stack local disponible. No representa cierre contractual ni publicación remota.

### Entorno y backup

- El `Access is denied` de Docker fue específico del sandbox; Docker Desktop 4.47.0 / Engine 28.4 quedó confirmado operativo mediante comprobación elevada.
- SQL Server, Keycloak, MinIO y ClamAV estaban activos en sus puertos locales; no se eliminaron contenedores ni volúmenes.
- Backup copy-only previo a la validación DB: `/var/opt/mssql/data/gestionasistenciadb_pre_p0h_20261024.bak`.
- `RESTORE VERIFYONLY` con `CHECKSUM`: PASS.

### Validación ejecutada

| Área | Comando/flujo | Resultado comprobado |
|---|---|---|
| DB runtime | `test_summary.ps1` | exit 0; 245 esperados, 249 ejecutados, 248 PASS, 0 FAIL, 1 skip permitido, 0 críticos ausentes, 0 errores SQL, `@@TRANCOUNT=0`, `DB GATE PASS` |
| Backend unitario | Maven verify con entorno cargado y `APP_SESIONES_V2_ENABLED` retirado | Surefire `2046/2046` PASS; sin fallos ni errores |
| Backend integración | Failsafe del mismo comando | `235` tests; `7` errors y `1` skip; no PASS global |
| Cobertura backend | JaCoCo del mismo comando | gates cumplidos; el build falla por las incidencias de integración/contexto |
| Frontend | `npm.cmd run verify` | exit 0; `807` PASS; lines `88,81 %`, branches `70,45 %`; realtime y producción PASS |
| JWT E2E | `utc-real-jwt-e2e.ps1` | `28/28 PASS` |
| soporte/binding E2E | `support_binding_e2e.py main` | `36/36 PASS`, `0` SQL NOT_RUN |
| sesiones E2E | `student_sessions_e2e.py` | `15/15 PASS`, `0` SQL NOT_RUN |
| idempotencia E2E | harness de soporte | `16/16 PASS`, con aviso informativo `GAP-D1.1` |
| runtime | health backend y raíz frontend | HTTP 200 en `8080` y `4200` |

### Incidencias que mantienen el estado no listo

1. `AcademicQueryJpaParityIT` falla porque `dbo.uv_coordinador` no expone `correo` en el runtime/source actual; la documentación HU121/HU122 apunta a una ampliación objetivo que requiere decisión DB.
2. Cuatro tests `SolicitudRevisionDocenteDetalleSqlIT` fallan porque `dbo.usp_consultar_detalle_solicitud_revision_docente` no existe en runtime ni en el checkout DB inspeccionado.
3. Dos `AuditHttpIT` fallan por composición de mocks: el mock de `GrupoRepositoryPort` reemplaza un bean que también debe satisfacer `GrupoFacultadQueryPort`.
4. D1/R2 y su resultset siguen sujetos al owner DB; el `DB GATE PASS` es local/experimental y no cambia el contrato oficial.
5. La alerta de seguridad concreta de PR #38 sigue sin ID o evidencia reproducible; queda `BLOCKED_BY_MISSING_EVIDENCE`.

No se modificaron tests para hacer pasar la ejecución, no se aplicó SQL ad hoc para ocultar los tres bloqueantes y no se imprimieron secretos. El archivo local generado `support-binding-e2e-state.json` se conserva para trazabilidad y no se elimina automáticamente.

**Dictamen actualizado:** `BLOCKED / PARTIALLY_READY — LOCAL EXPERIMENTAL`. No crear `CLOSURE.md` hasta resolver los conflictos/evidencias y ejecutar nuevamente la validación completa.

## Addendum de validación — Coordinator y Failsafe focalizado 2026-10-24

Se conserva todo el historial anterior. Esta sección registra únicamente la ejecución posterior.

| Área | Comando/alcance | Resultado actual |
|---|---|---|
| Coordinator | targeted Maven con controller/use case/mapper/ArchUnit | `35/35 PASS` |
| Compilación | `\.\mvnw.cmd -B -ntp -DskipTests compile` | `BUILD SUCCESS`, Java 25 |
| Backend completo | `\.\mvnw.cmd -B -ntp verify`, entorno cargado en memoria, `APP_SESIONES_V2_ENABLED=false` | `2047/2047 PASS`, JaCoCo PASS, `BUILD SUCCESS` |
| Failsafe focalizado | `AuditHttpIT`, `AcademicQueryJpaParityIT`, `SolicitudRevisionDocenteDetalleSqlIT` | `14` tests; `AuditHttpIT 2/2 PASS`; `5` errors de DB; build Failsafe no cero |
| Integración completa | `\.\mvnw.cmd -B -ntp -Pintegration verify`, mismo aislamiento UTC | Failsafe `235` tests, `5` errors, `1` skip; `AuditHttpIT 2/2 PASS`; JaCoCo PASS; build no cero |
| D06 local | metadata SQL Server | columna de procedencia y SP V2 presentes; sin cambio de esquema |
| Frontend/browser actual | sin checkout/proveedor frontend en este workspace | `NOT_RUN` |

### Resultado Failsafe

`AuditHttpIT` quedó resuelto con un mock que conserva también `GrupoFacultadQueryPort`. Permanecen cinco errores de proveedor: uno por la vista `dbo.uv_coordinador` sin `correo`/campos objetivo y cuatro por la ausencia de `dbo.usp_consultar_detalle_solicitud_revision_docente`. No se modificaron tests RED para acomodar la implementación ni se aplicó SQL ad hoc.

### UTC y ambiente

La validación reproducible forzó `APP_SESIONES_V2_ENABLED=false` solo en el proceso Maven para aislar el comportamiento versionado del `.env` ignorado. `application.yml` conserva `${APP_SESIONES_V2_ENABLED:false}` y las pruebas de guard OFF pasan. El cambio a default UTC/V2 `true` no se aplicó porque requiere confirmación explícita del cambio persistente y resolución de las fuentes contractuales `NOT_READY`; no hay fallback silencioso ni relectura histórica.

### Estado

`Coordinator` y la composición de `AuditHttpIT` quedan validados. La validación integral no se cierra: DB permanece `CONTRACT_CONFLICT / BLOCKED_BY_MISSING_EVIDENCE`, frontend/browser actual es `NOT_RUN`, y la decisión de default UTC continúa pendiente. No hubo escrituras remotas, merge, deploy ni exposición de secretos.

## Addendum de validación posterior — UTC V2 y reparación DB — 2026-10-24

Las conclusiones anteriores se conservan como historial de la línea base previa. Este addendum registra el estado posterior a la autorización explícita del usuario para activar UTC V2 por defecto en el entorno local.

### Diagnóstico y reparación de los cinco errores Failsafe

- Se inspeccionó `johnjduque/gestion-asistencia-db`, rama `develop`, commit `0604948ab9e4a5f6139ee66f4fb7925bf29cbbf3`. La fuente aprobada contiene `correo` y los campos de identidad/rol esperados en `schema/views/uv_coordinador.sql`, además de `schema/stored-procedures/usp_consultar_detalle_solicitud_revision_docente.sql`.
- La instancia local fue identificada como `gestionasistenciadb` en el servidor SQL Server local `2f41e9118416`; no se conectó a otra base. Antes de corregirla, `Coordinador` no tenía `estado`/`facultad`, `uv_coordinador` no tenía los campos ampliados, el procedimiento de detalle no existía y `ARCHIVO/ESTADO_ADJUNTO` no estaba sembrado.
- Se creó el respaldo `/var/opt/mssql/data/gestionasistenciadb_pre_utc_20261024_172506.bak` dentro del contenedor SQL Server. `RESTORE VERIFYONLY WITH CHECKSUM` terminó correctamente. El respaldo se conserva.
- Se aplicaron localmente, en orden y sin modificar el repositorio DB remoto, los scripts aprobados de `develop`: migración `05_hu121_hu122_coordinador_estado_facultad_up.sql`, las vistas `uv_coordinador_identidad.sql` y `uv_coordinador.sql`, `usp_consultar_detalle_solicitud_revision_docente.sql` y seed `16_hu035_parametro_estado_adjunto.sql`.
- La verificación posterior confirmó las columnas de coordinación/identidad, el procedimiento como `SQL_STORED_PROCEDURE` y `ARCHIVO/ESTADO_ADJUNTO=ATTACHED`.
- Failsafe focalizado: `AuditHttpIT`, `AcademicQueryJpaParityIT` y `SolicitudRevisionDocenteDetalleSqlIT`, 14 tests, 0 fallos, 0 errores, 0 skips, `BUILD SUCCESS`.
- Integración completa: `\.\mvnw.cmd -B -ntp -Pintegration verify`, 235 Failsafe tests, 0 fallos, 0 errores, 1 skip explícito, `BUILD SUCCESS`, finalizado `2026-10-24T17:48:02-05:00`.

El cambio de `CoordinadorJdbcBaseline` solo actualiza el oráculo de paridad para reflejar la vista aprobada desplegada; no oculta el desfase ni modifica pruebas RED para forzar un resultado.

### UTC V2 local y compatibilidad

- `application.yml` usa ahora `${APP_SESIONES_V2_ENABLED:true}`; la activación no depende de `localStorage` ni de una bandera manual en una instalación local limpia.
- El guard UTC-D06 verifica el contrato requerido al arrancar. Con el entorno cargado en memoria y sin `APP_SESIONES_V2_ENABLED` en el proceso Maven, `SesionUtcActivationGuardTest` terminó `5/5 PASS` y registró V2 habilitado por defecto.
- La compatibilidad V1 se conserva cuando se solicita explícitamente `app.sesiones.v2.enabled=false`; el interceptor 404 solo se instala en ese opt-out. Las fechas V1 con offset siguen rechazándose; no se reinterpretan históricos ni se introducen fallbacks silenciosos.
- `-Pintegration verify` ejecutó las pruebas de creación, consulta, actualización y paridad de sesiones contra SQL Server local con V2 por defecto. JaCoCo terminó con LINE `93.24%` y BRANCH `81.88%`, superando LINE `80%` y BRANCH `70%`.

### Alcance no ejecutado y decisiones D1/R2

- Se revisaron `C:\Users\josev\AsistenciasUCO\AsistenciasUCO-Frontend`, los worktrees del proyecto y `C:\Users\josev\.codex\worktrees`; no hay checkout frontend utilizable ni `package.json`. Las carpetas encontradas en `.codex\worktrees` son placeholders sin fuente. Por ello las pruebas frontend y la validación visual en navegador de Coordinador, Docente, Estudiante, sesiones, horarios, asistencias y reclamos quedan `NOT_RUN / BLOCKED_BY_MISSING_EVIDENCE`, no PASS.
- La suite nativa del repositorio DB no se ejecutó porque ese checkout no está disponible localmente; la evidencia DB ejecutada corresponde a la instancia SQL local mediante las pruebas de integración del backend y la inspección directa de objetos.
- La vista/coordinador ampliada y el procedimiento de detalle están respaldados por `develop`, pero el procedimiento está clasificado por la fuente DB como extensión `NON_GOLDEN_PATH_EXTENSION`, y D1/R2 permanecen sujetos al owner DB. La aplicación local está validada contra ellos; no se declara aprobado el contrato oficial, el freeze ni un despliegue remoto.

### Seguridad y preservación

`.env` está ignorado por `.gitignore` y no está versionado. Sus valores nunca se imprimieron. Se conservaron el respaldo SQL, los datos de prueba existentes y `support-binding-e2e-state.json`. No hubo push, merge remoto, cambio remoto ni despliegue de producción.

**Veredicto del work item:** backend y DB local quedan técnicamente validados, incluidos los cinco errores Failsafe reproducibles y UTC V2 por defecto. El cierre funcional completo y el inicio de la siguiente fase siguen bloqueados por la ausencia del checkout/frontend para pruebas reales de navegador y por la aprobación contractual D1/R2 del owner DB.

## Addendum final de consolidación local — 2026-10-24T19:28:56-05:00

Esta sección agrega la evidencia de la consolidación local de los cuatro proyectos. No reescribe los históricos anteriores ni convierte una validación local en aprobación contractual o publicación remota.

### Validación ejecutada sobre el estado final local

| Área | Comando/alcance | Resultado actual |
|---|---|---|
| Backend unitario, arquitectura y JaCoCo | mvnw.cmd -B -ntp verify mediante scripts/test-local.ps1, Java 25 y .env cargado solo en proceso | 2051 tests, 0 fallos, 0 errores, 0 skips; build completo SUCCESS |
| Backend integración real | mvnw.cmd -B -ntp -Pintegration verify dentro del mismo wrapper, SQL Server/Keycloak/MinIO/ClamAV existentes | 235 Failsafe, 0 fallos, 0 errores, 1 skip permitido; build SUCCESS |
| Backend cobertura | JaCoCo generado por el mismo wrapper | LINE 93,24 %; BRANCH 81,88 %; gates 80/70 cumplidos |
| Frontend | npm.cmd run verify sobre el checkout oficial | 1044 tests ChromeHeadless, 0 fallos; cobertura global LINE 90,17 %, BRANCH 72,29 %; realtime LINE 95,06 %, BRANCH 84,52 %; build producción SUCCESS |
| DB despliegue local | deploy_schema.ps1 -ContainerName sql_server_asistencias | exit 0; objetos versionados aplicados sobre gestionasistenciadb, sin borrar/recrear la base |
| DB migración HU091 | migración versionada migrations/05_hu091_docente_programa_tipo_vinculacion.sql | exit 0; retiro del procedimiento obsoleto aplicado desde fuente versionada |
| DB quality gate | test_summary.ps1 -ContainerName sql_server_asistencias | TOTAL_EXECUTED=300, PASSED=299, FAILED=0, SKIPPED=1, ALLOWED_SKIPPED=1, CRITICAL_MISSING=0, SQLCMD_EXIT_CODE=0, SQL_ERROR_COUNT=0, @@TRANCOUNT=0; DB GATE PASS |
| Salud local | Invoke-WebRequest a frontend raíz y /actuator/health | frontend HTTP 200; backend HTTP 200; /v3/api-docs sin credencial respondió 401 (esperado por protección) |

La prueba visual/manual de navegador queda NOT_RUN: Computer Use no pudo determinar con seguridad el URL de la pestaña Edge existente y se detuvo antes de cualquier clic, escritura o autenticación. No se forzó la barrera. Además, el servidor que responde en localhost:4200 pertenece a un frontend quarantine activo, no se atribuye como evidencia del checkout oficial.

### Reconciliación y limpieza

- Se actualizaron por fast-forward los refs locales con los remotos personales configurados: backend origin/develop=1b62498c..., frontend legacy-frontend/develop=ecaba728... y DB legacy-db/develop=0604948a.... Infraestructura avanzó localmente de a544c49... a 54ace6c... desde origin/develop; no tiene remoto personal configurado.
- Se conservaron los cambios funcionales locales ya existentes en backend/frontend; se añadió únicamente aislamiento explícito app.sesiones.v2.enabled=false en SesionUtcActivationGuardTest para que el test de opt-out no dependa del .env local.
- Se eliminaron cinco clones duplicados verificados bajo .workspace: db-cc003g01, db-readonly, db-utc-d06, frontend-p0b y gestion-asistencia-db. Todos estaban sin cambios útiles no incorporados; el primero solo tenía un log generado.
- Se eliminaron los dos logs temporales no usados de .workspace/fase1. Se conservaron los SQL/diagnósticos de fase1 por ser artefactos locales no versionados potencialmente únicos.
- Se conservaron support-binding-e2e-state.json, db-backups, security-baseline-local y los worktrees ajenos al proyecto. No se crearon contenedores, volúmenes ni infraestructura nueva.
- El quarantine histórico C:/Users/josev/AsistenciasUCO-P0D-quarantine-2026-10-20 no se eliminó completamente: conserva AsistenciasUCO-Frontend-p0b con tres cambios no confirmados y gestion-asistencia-db-workspace en una historia no contenida en el checkout DB oficial. Su clon frontend frontend-lb004d-working no pudo borrarse porque sus logs están abiertos por procesos Node/esbuild activos; no se detuvieron procesos ni se forzó la eliminación.

### Cierre y límites

No hubo push, PR, merge remoto, force-push ni publicación. DB e infraestructura quedaron limpios; backend y frontend mantienen el working tree local documentado por P0-H/MAINT-004. El dictamen permanece LOCAL EXPERIMENTAL / PARTIALLY_READY: la evidencia técnica local es verde, pero el cierre DoD sigue bloqueado por la validación visual/manual no ejecutada, la aprobación contractual D1/R2 del owner DB y los artefactos únicos que permanecen en quarantine.

## Addendum final de validación funcional real y limpieza — 2026-10-24

Este addendum supersede únicamente las conclusiones anteriores que decían que el navegador real no se había ejecutado o que `4200` pertenecía a quarantine. Los históricos y sus límites se conservan.

### Procesos oficiales y servicios

- Frontend oficial activo en `C:\Users\josev\OneDrive\Documentos\Front_Asistecias\AsistenciasUCO-Frontend`: PID `39516`, `node.exe`, Angular CLI servido desde ese checkout; `http://127.0.0.1:4200/login` respondió HTTP `200`.
- Backend oficial activo en `C:\Users\josev\AsistenciasUCO\AsistenciasUCO`: PID `40876`, Java 25; `http://127.0.0.1:8080/actuator/health` respondió HTTP `200`.
- Permanecen activos los servicios locales necesarios ya existentes: SQL Server, Keycloak, MinIO, ClamAV y observabilidad. No se crearon contenedores, volúmenes, clones, worktrees ni proyectos de automatización nuevos.

### Navegador real independiente y matriz de roles

Playwright no estaba instalado en el checkout oficial y no se agregaron dependencias. Como Edge existente no ofreció una sesión utilizable para esta comprobación, se usaron sesiones independientes de Edge/CDP con perfiles temporales, mediante login normal del formulario. No se inyectaron tokens, `localStorage` ni estados simulados; no se imprimieron credenciales ni secretos.

| Rol | Resultado observable | Red/consola |
|---|---|---|
| Docente | PASS: login, `/app/docente/grupos`, módulo docente, grupo visible y rutas de asignaturas/horarios HTTP `200` | sin fallos de red ni errores de consola capturados |
| Estudiante | PASS: login, `/app/estudiante/horarios`, módulo estudiante y endpoint de horarios HTTP `200`; la cuenta no tenía clases registradas | sin fallos de red ni errores de consola capturados |
| Coordinador | PASS: login, `/app/coordinador/docentes`, lista/paginación y endpoints de docentes, tipos de identificación y vinculación HTTP `200` | sin fallos de red ni errores de consola capturados |
| Administrador | PASS: login, `/app/admin/decanos`, lista de decanos y endpoints de facultades/decanos/docentes/tipos HTTP `200` | sin fallos de red ni errores de consola capturados |
| Decano | `NOT_RUN / BLOCKED_BY_MISSING_EVIDENCE`: Keycloak no tiene usuario habilitado con rol DECANO ni identidad E2E aprobada para crear | no se inventó identidad ni contraseña |

### Flujo Docente de sesiones, zona horaria y asistencia

- Desde la UI oficial, Docente abrió Toma de Asistencia, creó la sesión marcada `E2E-UI-UTC-20261024` y recibió HTTP `201` con toast de creación correcta. La solicitud llevó `America/Bogota` y offsets `-05:00`; no hubo uso de `localStorage`.
- La UI abrió la sesión, mostró estudiantes, registró una asistencia con HTTP `201`, mostró el mensaje de éxito y sobrevivió a `reload`; después de recargar las lecturas asociadas respondieron HTTP `200` sin fallos de red.
- La UI oficial no expone control de edición temporal de sesión ni ruta de edición. Por tanto, el PATCH temporal no se simuló ni se añadió funcionalidad: quedó validado en backend mediante `utc-real-jwt-e2e.ps1`, `28/28 PASS`, incluyendo igualdad de instante, offsets, UTC V2, ownership, payloads inválidos y regresiones.
- La sesión y sus filas dependientes creadas para la prueba fueron eliminadas mediante limpieza SQL transaccional exacta; la comprobación posterior dejó cero filas con el marcador. No se tocó histórico ajeno.

### Reclamos y soportes

- Las cuentas dedicadas de soportes existen, están habilitadas y tienen rol/`idUsuario` coherentes; sin embargo, la emisión de token actual devuelve HTTP `400 invalid_grant`. El provisionador oficial confirmó las cuentas, pero los resets locales fueron rechazados por la política de historial de contraseñas. La prueba actual `support_binding_e2e.py main` queda `BLOCKED / NOT_RUN`, no `PASS`.
- La evidencia histórica de `34/34 PASS` no se reutiliza como resultado actual. El estado E2E existente se conservó; su limpieza SQL terminó correctamente y los tres objetos MinIO ya estaban ausentes (`ABSENT=3`, sin errores de comando).
- El frontend oficial mantiene `teacherClaimsEnabled=false` y documenta el módulo docente de reclamos como backend `501 / FEATURE_UNAVAILABLE`; sus tests verifican que el `501` se propaga y no se convierte en lista vacía. Por ello no se declara PASS para crear reclamo, adjuntar soporte, consultar/descargar/aprobar/rechazar ni ver el estado del estudiante. No se cambió D1/R2 ni se inventó un endpoint.

### Consolidación y preservación de checkouts

La cuarentena `C:\Users\josev\AsistenciasUCO-P0D-quarantine-2026-10-20` contiene ahora únicamente:

- `AsistenciasUCO-Frontend-p0b`, preservado por cambios locales únicos no confirmados.
- `gestion-asistencia-db-workspace`, preservado por historia/trabajo DB no contenido en el checkout oficial.

Se eliminaron solamente `frontend-lb004d-working` (sin trabajo único; sus procesos Node/esbuild fueron detenidos antes) y `historical-e2e-logs` (solo logs). No se eliminaron los dos checkouts preservados ni se creó un clon de reemplazo.

Estado final comprobado de los cuatro repositorios:

- Backend: `jose-valencia/p0-h-backend-functional-integration`, `6623249772fc77b28956a277708bc8dbcf489d06`, `2` commits adelante y `0` atrás de `origin/develop`; cambios locales preexistentes documentados.
- Frontend: `jose-valencia/p0-h-frontend-functional-integration`, `ecaba7280fc20324e9cb81876b62bc1c7046b6da`, alineado con `legacy-frontend/develop`; cambios locales preexistentes documentados. `git diff --check` no reportó errores, solo avisos LF/CRLF.
- DB: `jose-valencia/p0-h-db-functional-integration`, `26d64eb28467d8ae59f1d53a6c28dd188fb5b005`, `2` commits adelante y `0` atrás de `legacy-db/develop`, limpio.
- Infra: `main`, `54acee6c4fb12de11b2b3dc4ea7ce6b6ec3c87a0`, alineado con `origin/develop`, limpio.

### Dictamen

La aplicación local oficial queda funcional para Docente, Estudiante, Coordinador y Administrador en los flujos comprobados; UTC V2, permisos y backend están validados. No se declara cierre DoD: Decano permanece sin identidad aprobada, el flujo actual de reclamos/soportes no puede certificarse por `invalid_grant` y backend `501`, la edición temporal no existe en la UI oficial, y D1/R2 siguen pendientes del owner DB. No hubo push, PR, merge remoto, force-push, despliegue ni incorporación de secretos.

## Addendum final — reclamos, Keycloak, Decano y regresión local — 2026-10-24

Este addendum supersede las conclusiones anteriores sobre `invalid_grant`, el supuesto `501` de reclamos docentes y el estado del E2E de soportes. Mantiene sin cambios los límites contractuales y los bloqueos que todavía requieren decisión humana.

### `invalid_grant`: causa y corrección

- El realm usado por el harness y por el frontend es `asistencias-uco`, con el cliente público `asistencias-uco-frontend`; ambos usan Resource Owner Password Credentials autorizado localmente. El harness no envía `scope`; el frontend envía `openid email profile`. Tras la corrección, ambos caminos emitieron tokens HTTP `200`, por lo que no se comprobó una divergencia harness/Edge como causa.
- La causa fue local: deriva de credenciales entre los tres archivos ignorados de E2E y bloqueo por protección de fuerza bruta de las cuentas dedicadas. Keycloak mostraba fallos acumulados/bloqueo temporal y eventos de credenciales inválidas; no había `requiredActions` pendientes.
- Se alinearon únicamente las cuatro cuentas sintéticas de soporte con credenciales ya existentes en los fixtures autorizados, se limpió su estado de fuerza bruta y se aplicó el reset oficial de Keycloak. No se cambiaron contraseñas de usuarios reales, no se habilitó un flujo inseguro global y no se imprimieron secretos.
- El provisioner oficial confirmó las cuatro cuentas como existentes y con los roles esperados. Sus `idUsuario` locales quedaron alineados con usuarios presentes en SQL Server; esto elimina el segundo fallo observado (`ERR_USUARIO_NO_EXISTE`) sin crear filas SQL.
- `infra/keycloak/.env` sigue ignorado; la revisión de sus parámetros no mostró cambio de realm/cliente/puertos ni activación de reset persistente. Sus valores secretos no se incorporan a esta evidencia.

### Reclamos docentes y soportes

- El AS-IS real de `DocentePortalController` es: `GET /api/v1/docente/reclamos`, `GET /api/v1/docente/reclamos/{id}` y `PATCH /api/v1/docente/reclamos/{id}` implementados mediante puertos de aplicación. La respuesta `501 FEATURE_UNAVAILABLE` explícita del controlador corresponde a `PATCH /api/v1/docente/sesiones/{sesionId}/cancelar`, no a reclamos.
- Con JWT Docente válido, el listado filtrado respondió `200` y el listado sin filtro respondió `200`; el detalle de un reclamo existente respondió `200`. Los endpoints protegidos del estudiante no se confundieron con estos resultados: un Docente recibió `403` al intentar `/api/v1/estudiante/reclamos`, mientras el `GET` como Estudiante continúa respondiendo `501 FEATURE_UNAVAILABLE`.
- El frontend tenía una bandera obsoleta. Se cambió únicamente `teacherClaimsEnabled` a `true` en `src/environments/environment.ts`; no se duplicaron controladores ni se cambió el contrato. El componente conserva el mensaje `501` solo como manejo de error real, no como estado por defecto.
- El flujo oficial existente de binding de soportes quedó certificado: `support_binding_e2e.py main` terminó con `36 checks, 36 PASS, 0 FAIL, 0 SQL NOT_RUN`. Incluye 401/403, PDF válido, ClamAV limpio, almacenamiento MinIO, fila SQL `DRAFT`, radicación `202`, transición `ATTACHED`, lectura/descarga del titular y propietario, aislamiento de docente ajeno/estudiante/coordinador/administrador, doble radicación, entradas inválidas, EICAR, tamaño/tipo/nombre y limpieza SQL/MinIO.
- Esto no permite declarar completo el flujo solicitado de alta desde `/api/v1/estudiante/reclamos`, aprobación/rechazo y lectura posterior del estado del estudiante: las tres operaciones de reclamos del estudiante siguen en `501` en el backend actual. No se mutaron reclamos históricos para forzar una prueba de `PATCH`; la resolución queda respaldada por controller/application tests y por el endpoint real inspeccionado, pero no certificada como E2E de aprobación/rechazo.

### Decano

- La base local contiene el Decano institucional existente y su facultad, pero Keycloak no tiene una identidad habilitada con rol `DECANO` ni existe una cuenta E2E aprobada para esa relación.
- La provisión de una identidad sintética por el mecanismo oficial fue intentada sin bypass: el endpoint devolvió `404 ERR_USUARIO_NO_EXISTE` antes de crear la fila. No se escribió directamente en SQL ni se usó una contraseña de un usuario institucional real.
- Resultado: validación navegador del quinto rol `NOT_RUN / BLOCKED_BY_MISSING_EVIDENCE`. No se puede declarar aislamiento por facultad ni módulos de Decano hasta disponer de una identidad aprobada y coherente en Keycloak/SQL.

### D1/R2: clasificación y decisión pendiente

- W1 `usp_registrar_archivo_soporte` y W2 `usp_radicar_solicitud_revision_asistencia_v2` son mutaciones con resultset de cuatro columnas, compatible con el baseline de mutaciones.
- R1 es una lectura autorizada y R2 `usp_consultar_solicitudes_revision_docente` es una query paginada; la fuente SQL actual expone 27 columnas de lectura, incluyendo `totalRegistros`. No se debe recortar una lectura de 27 columnas al shape de cuatro columnas de mutaciones.
- La matriz `CONTRACT_MATRIX.md` fue corregida de 25 a 27 como dato AS-IS observado. La validación local respalda consumidores y ownership, incluida la corrida de soportes, pero no convierte la extensión `NON_GOLDEN_PATH_EXTENSION` en contrato aprobado.
- Propuesta para el owner DB: aprobar explícitamente W1/W2 como resultsets de mutación de cuatro columnas y R1/R2 como resultsets de lectura versionados, con nombres, columnas, orden, nulabilidad, paginación, permisos y consumidor backend congelados; o emitir una decisión alternativa versionada. La aprobación no existe todavía.
- El bloqueo D1/R2 afecta la promoción formal y el cierre DoD; no invalida la funcionalidad local que ya se ejecuta contra la instancia preparada.

### Regresión y limpieza

| Área | Validación | Resultado |
|---|---|---|
| Frontend oficial | `npm.cmd run verify` | exit `0`; 1044 ChromeHeadless PASS; LINE `90,17 %`; BRANCH `72,29 %`; realtime PASS; build producción PASS |
| Backend | último `mvnw.cmd verify` ya ejecutado sin cambios backend en esta continuación | 2051 tests unitarios PASS; Failsafe 235, 0 fallos, 0 errores, 1 skip permitido; JaCoCo LINE `93,24 %`, BRANCH `81,88 %` |
| DB | quality gate local ya ejecutado; soporte repetido contra la misma instancia | 300 ejecutados, 299 PASS, 0 FAIL, 1 skip permitido; soporte `36/36 PASS` |
| UTC V2 | configuración local oficial y E2E existente | permanece por defecto; `utc-real-jwt-e2e.ps1` `28/28 PASS` |
| Servicios | backend/frontend oficiales y Keycloak/SQL/MinIO/ClamAV existentes | sin contenedores, bases, clones, worktrees ni servidores Angular nuevos |

Los dos checkouts históricos restantes fueron inspeccionados en solo lectura. El checkout frontend conserva tres cambios locales no confirmados y el checkout DB no presenta cambios útiles pendientes; ambos se preservan por seguridad. No se creó otra cuarentena ni se eliminó información única. El estado temporal `support-binding-e2e-state.json` generado por la corrida exitosa fue eliminado tras comprobar la limpieza del E2E.

**Veredicto histórico de ese corte — supersedido por el addendum correctivo del 2026-10-25:** `PARTIALLY_READY`. `invalid_grant` estaba resuelto y el binding de soportes estaba verde; el frontend ya no ocultaba la bandeja docente implementada. La referencia a que faltaban el alta/estado de reclamos del estudiante y el E2E de aprobación/rechazo era una conclusión incompleta: confundía las rutas legacy `501` con el recorrido vigente `POST /api/v1/asistencias/revisiones`. Se conserva aquí solo para trazabilidad; el dictamen aplicable es el addendum correctivo posterior. No hubo push, PR, merge remoto, force-push, despliegue ni exposición de secretos.

## Addendum correctivo — recorrido real de reclamos y soportes — 2026-10-25

Este addendum corrige la conclusión anterior que confundía la API legacy de reclamos del estudiante con el recorrido actualmente implementado. No se reescribe la evidencia histórica ni se introduce un endpoint nuevo.

### Evidencia recuperada del 20 de octubre

La documentación existente conserva el contrato y parte de la evidencia técnica, pero no contiene un artefacto versionado de navegador que pruebe específicamente la aprobación y el rechazo manual del 2026-10-20. La declaración de esa prueba manual queda registrada como evidencia aportada por el solicitante, no como una corrida automatizada recuperable del repositorio.

| Fuente existente | Evidencia que sí contiene | Límite que se conserva |
|---|---|---|
| `P0-G-recuperacion-funcional/CONTRACT_MATRIX.md` | W1/W2/R1/R2 y las rutas `POST /api/v1/archivos/subir`, `POST /api/v1/asistencias/revisiones`, `GET /api/v1/docente/reclamos` y `GET /api/v1/archivos/{fileId}` | contrato/histórico; no sustituye una corrida actual |
| `P0-I-cierre-integracion-final/PLAN.md` | tres radicaciones manuales HTTP `202` del 20-10 y conteo de solicitudes en SQL | no documenta el clic de aprobar/rechazar en UI |
| `P0-I-cierre-integracion-final/VALIDATION.md` | suite de backend/frontend y fixtures de SQL | el E2E HTTP de estudiante estaba `NOT_RUN` por `invalid_grant` en esa fecha |
| `P0-E-consolidacion-funcional/VALIDATION.md` | declara que no se ejecutó una mutación E2E de reclamo/soporte en ese corte | contradice una certificación automatizada completa; no se resuelve por suposición |
| `HU035-HU036-bandeja-detalle-reclamos-docente/VALIDATION.md` | E2E backend posterior `17/17 PASS`, listado/detalle, radicación y descarga | no certifica aprobación/rechazo desde navegador |

### Recorrido HTTP real y estado actual

El recorrido vigente no usa `POST /api/v1/estudiante/reclamos`. Las peticiones observadas son:

1. Estudiante: `GET /api/v1/estudiante/materias` y `GET /api/v1/estudiante/materias/{materiaId}/sesiones`.
2. Estudiante: `POST /api/v1/archivos/subir` con PDF; respuesta `201` y `fileId`.
3. Estudiante: `POST /api/v1/asistencias/revisiones` con `sesionId`, categoría, justificación y `soporteArchivoId`; respuesta `202`.
4. Docente titular: `GET /api/v1/docente/reclamos?estado=P&pagina=1&tamano=...`; respuesta `200`.
5. Docente titular: `GET /api/v1/docente/reclamos/{id}` y `GET /api/v1/archivos/{fileId}`; la descarga devolvió `200` y SHA-256 idéntico al PDF subido.
6. Docente titular: `PATCH /api/v1/docente/reclamos/{id}` con `accion=APROBADA` o `RECHAZADA`; respuesta `200` y el listado posterior reflejó `APROBADA`/`RECHAZADA`.
7. Estudiante: vuelve a ejecutar `GET /api/v1/estudiante/materias/{materiaId}/sesiones` después de refrescar.

La repetición HTTP contra los servicios oficiales y dos cuentas E2E corrigió la hipótesis de ausencia de reclamos:

| Tramo | Resultado actual |
|---|---|
| `student_sessions_e2e.py` | `15/15 PASS`, `0` SQL `NOT_RUN`; fixture `E2E-FIX-A/B` limpiado |
| Soporte/binding oficial `support_binding_e2e.py main` | `34/34 PASS`, `0` FAIL, `0` SQL `NOT_RUN`; PDF, ClamAV, MinIO, DRAFT, `202`, ATTACHED, bandeja, descarga, aislamiento y limpieza |
| Dos estudiantes, subida/radicación/listado/descarga | `201`, `202`, `200`, `200` y SHA idéntico para cada cuenta en sesiones aisladas |
| Resolución aprobada | `PATCH 200`; docente ve `APROBADA`; primera comprobación fallida aparece en el GET de refresco del estudiante |
| Resolución rechazada | El endpoint y el controlador están implementados; no se declara PASS del estado estudiantil separado porque la corrida se detuvo en el primer fallo de aprobación |
| Navegador UI actual | `NOT_RUN`: Computer Use terminó antes de la navegación porque Edge no expuso una URL identificable; no se inventa un PASS visual |

### Primera discrepancia y causa comprobada

La primera petición HTTP que no entrega el comportamiento esperado no es un `501`: es el `GET /api/v1/estudiante/materias/{materiaId}/sesiones` posterior a una aprobación. La respuesta fue HTTP `200`, con `estadoSolicitudRevision=aceptada`, pero `estadoAsistencia=NO_DISPONIBLE` y `reclamable=false`, cuando el contrato F3 espera `JUSTIFICADA` tras la resolución aprobada.

La traza SQL aislada comprobó que el `PATCH` sí persistió `DetalleAsistencia.asistio=1`. El procedimiento vigente `usp_resolver_solicitud_revision_asistencia` conserva `codigoRazonCausa=SJC`; el proyector backend clasifica `asistio=true` como `PRESENTE` solo si la razón es `AN` y, para `SJC`, cae en `NO_DISPONIBLE` (`ConsultarSesionesMateriaEstudianteUseCaseImpl`, `resolverEstado`/`clasificar`). Por tanto, existe una regresión o desalineación real de la semántica post-aprobación: no es una ruta equivocada ni justifica volver a desarrollar radicación, soporte, bandeja o resolución.

No se modificó código ni contrato en esta validación porque la instrucción vigente fue actualizar únicamente documentación. El arreglo técnico mínimo queda pendiente de un work item/decisión de contrato que alinee la transición aprobada con la proyección estudiantil, con RED y validación antes de implementarlo.

### Legacy y retiro/eliminación

El controlador actual mantiene como `501 FEATURE_UNAVAILABLE` las rutas legacy:

- `GET /api/v1/estudiante/reclamos`.
- `POST /api/v1/estudiante/reclamos`.
- `DELETE /api/v1/estudiante/reclamos/{id}`.

Estas rutas no participan en la radicación actual. `DELETE /api/v1/estudiante/reclamos/{id}` sí corresponde a la capacidad adicional de retirar/eliminar una solicitud que la UI intenta ofrecer; esa capacidad queda pendiente y no debe confundirse con crear, adjuntar, listar, descargar, aprobar o rechazar.

**Dictamen corregido:** `PARTIALLY_READY`. Se retira la conclusión incorrecta de que falte desarrollar nuevamente el flujo de reclamos. El flujo vigente llega hasta la resolución y demuestra soporte real; queda abierta una regresión comprobada en la lectura de asistencia del estudiante después de aprobar, la validación visual actual y la capacidad independiente de retiro/eliminación. D1/R2 sigue siendo un asunto contractual separado.

## Addendum de continuidad — 2026-10-26

### Backend y guard de sincronización privilegiada

Se continuó sobre el checkout existente, sin resetear, limpiar ni sobrescribir cambios previos. El `HEAD` al inicio de esta validación fue `5d591d19dd8c0285027e5f70e04e8094f977e2ab`; no se creó commit ni se hizo push en esta continuación.

La configuración del cliente administrativo del PDP quedó protegida con una bandera independiente:

- `SECURITY_BASELINE_PDP_PROFILE_SYNC_ENABLED` tiene fallback `false`.
- El secreto del PDP se lee únicamente desde `SECURITY_BASELINE_PDP_ADMIN_CLIENT_SECRET`.
- No se reutiliza `SECURITY_BASELINE_IDENTITY_ADMIN_CLIENT_SECRET`.
- No se modificaron audiencias, permisos ni credenciales externas.

Esto deja la sincronización privilegiada deshabilitada hasta contar con evidencia de autorización efectiva en el PDP. Los tests del cliente simulado certifican únicamente el comportamiento local del adapter; no certifican el proveedor externo.

### Comandos ejecutados

| Fecha/hora local | Comando | Resultado | Evidencia |
|---|---|---|---|
| 2026-10-26 16:59:15 -05 | `$env:JAVA_HOME='C:\Program Files\Java\jdk-25'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd -B -ntp -Dtest='SecurityBaselinePdpAdminPropertiesTest,SecurityBaselinePdpProfileClientTest,ProfileSyncingIdentityProviderTest,IdentityAdapterSelectionTest' test` | `PASS`, 38/38, 0 fallos, 0 errores, 0 omitidas, exit 0 | Surefire |
| 2026-10-26 16:55:00 -05 | `$env:JAVA_HOME='C:\Program Files\Java\jdk-25'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd -B -ntp clean verify` sobre el estado previo al guard | `PASS`, 2301/2301, 0 fallos, 0 errores, 0 omitidas, exit 0 | Surefire, ArchUnit, JaCoCo |
| 2026-10-26 17:04:47 -05 | `$env:JAVA_HOME='C:\Program Files\Java\jdk-25'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd -B -ntp clean verify` sobre el estado actual | `PASS`, 2302/2302, 0 fallos, 0 errores, 0 omitidas, exit 0 | Surefire, ArchUnit, JaCoCo |

La corrida completa previa al guard obtuvo JaCoCo: LINE `91,21 %`, BRANCH `81,74 %`, CLASS `90,55 %`, METHOD `88,58 %`, INSTRUCTION `91,67 %`; los gates configurados se cumplieron. La corrida actual posterior al guard obtuvo LINE `91,21 %`, BRANCH `81,78 %`, CLASS `90,55 %`, METHOD `88,58 %`, INSTRUCTION `91,67 %`; los gates configurados también se cumplieron.

### Estado remoto y límites

La consulta remota de solo lectura confirmó que los PR personales DB #49, Backend #43 y Frontend #12 están `OPEN`, `merged=false`, `mergeable=true`; no se fusionaron en esta continuación. Sus checks visibles terminaron en éxito, pero la evidencia remota no sustituye las validaciones faltantes: no hay estados HTTP reales contra el PDP/Keycloak, SQL Server habitual, frontend oficial ni navegador en este checkout.

Se mantienen como `NOT_RUN` o `BLOCKED_BY_MISSING_EVIDENCE` la base habitual, el proveedor PDP real, la validación de autorización administrativa, el frontend oficial, la prueba visual y la integración E2E cross-repository. El backend local queda técnicamente verde en la suite ejecutada; el cierre funcional y la preparación productiva no se elevan a `READY`.
