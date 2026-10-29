# VALIDATION — LOCAL-STABILIZATION-LB004D-2026-10-20

Estado: `IN_PROGRESS`; cierre funcional aún bloqueado por el conflicto contractual frontend y por la validación remota pendiente.

Este archivo se completa con comandos, fecha/hora local, exit code, alcance, cobertura, skips y dictamen. No almacenar tokens, contraseñas, payloads de archivos ni logs crudos.

## Resumen

| Área | Estado | Evidencia | Limitación |
|---|---|---|---|
| Git / refs | `PASS` parcial | Fetch/read-only completado; worktrees auxiliares: backend mergeado con `origin/develop`, DB mergeado con `origin/develop`; backend publicado en `7a2a5a9`, DB publicado en `bdb2935`; PR heads originales preservados | Workflows remotos disparados y aún `in_progress` |
| Fixture DB CI | `PASS` local | Fixture idempotente namespaced; test dirigido: 17/17; quality gate: Expected 245, Executed 248, Passed 247, Failed 0, Skipped 1 permitido, CriticalMissing 0, SQLCMD 0, SQL errors 0, unauthorized skips 0; cleanup PASS | La ejecución fue sobre SQL Server Developer local; CI remoto pendiente |
| Backend hygiene / verify | `PASS` | Java 25; `mvnw test` exit 0: 1651 tests, 0 failures/errors/skips; `mvnw verify` exit 0; JaCoCo LINE 91.05%, BRANCH 79.98%; `PYC_TRACKED=0` | Se observaron WARN/logs de tests, sin gate rojo |
| Frontend build | `PASS` | `npm ci` exit 0; `npm run build -- --configuration=production` exit 0 | No implica PASS de Karma/E2E |
| Frontend verify | `FAIL / BLOCKED` | `npm run verify` exit 1 por specs tipados preexistentes y `ChromeHeadlessCI` sin load; no se modificaron tests | Requiere decisión del owner de contratos/tests y navegador CI/local |
| Docker/providers | `PASS` parcial / `REVIEW_REQUIRED` | Keycloak readiness/realm 200; MinIO primario healthy loopback; ClamAV healthy; EICAR `FOUND`; archivo benigno `OK`; SQL running; legacy MinIO detenido sin borrar volumen | E2E backend real aún no certificado; legacy ClamAV histórico exit 137 |
| Angular E2E real | `NOT_RUN` | No se declara PASS sin browser, login, fixture contractual y binding DB docente | `REVIEW_BINDING` sigue `BLOCKED_BY_DB_CONTRACT` |

## Comandos

### 2026-10-20 — comandos ejecutados

- `git fetch --all --prune` en los tres repositorios: refs remotas actualizadas; no hubo merge remoto ni force-push.
- `.\mvnw.cmd test`, `.\mvnw.cmd verify` y `.\mvnw.cmd clean verify -q` en `be-stab`, Java 25: exit 0.
- Fixture + `test_lb004d_support_binding.sql` en `sql_server_asistencias`: exit 0; cleanup exit 0.
- Fixture + `test_summary.ps1` en `sql_server_asistencias`: exit 0; contadores registrados arriba; cleanup exit 0.
- `npm ci --no-audit --no-fund` en `fe-stab`: exit 0.
- `npm run verify` en `fe-stab`: exit 1; fallo reproducido y no convertido en PASS.
- `npm run build -- --configuration=production` en `fe-stab`: exit 0.
- Docker health/readiness y probes ClamAV: resultados registrados arriba.
- Workflows remotos observados tras el push: backend CI `39378510550`, Backend Security `39378510466`, Backend Deep Security Scan `39378510604`; DB Quality Gate `39378519111`, todos `in_progress` al momento de la consulta.

No se guardaron credenciales, tokens ni logs crudos.

### 2026-10-28 — reinicio sobre ramas `develop`

- Backend: checkout `develop` en `fcc0e7db`, actualizado con `origin/develop`; runner `scripts/start-local-stack.ps1 -NoDependencies -NoFrontend -WaitSeconds 120`; puerto 8080 y `/actuator/health` HTTP 200.
- Frontend principal: checkout `develop` en `992e3ff`, actualizado desde `legacy-frontend/develop`; runner local; puerto 4200 y `/app/admin/seguridad` HTTP 200.
- Frontends de seguridad: sus repositorios no publican rama `develop`; se preservaron sus cambios locales y se ejecutaron desde `main` (`securityBaseline-vue` `5a3c51e`, `security-ui` `6e6ed9b`); puertos 5173 y 5175 HTTP 200.
- Dependencias existentes: `sql_server_asistencias` en 1433, Keycloak legacy en 8081, Keycloak baseline en 19090, PDP/PEP en 18080/18081, MinIO en 9000 y ClamAV en 3310; probes HTTP aplicables HTTP 200 y contenedores activos/saludables. `minio-init` finalizó con exit 0 como servicio one-shot.
- Docker Server 28.4.0. No se ejecutaron migraciones, cambios de esquema, recreaciones, limpieza de volúmenes, cambios de permisos ni rotación de secretos.
- Verificador oficial de frontends: `PASS` para dependencias, puertos, Vue, Security UI, Angular, PDP, PEP y discovery de Keycloak.

Resultado: entorno local levantado sobre `develop` para backend y frontend principal, con los módulos de seguridad preservados en sus únicas ramas disponibles. Las certificaciones E2E/autorización contractual siguen fuera de alcance de este reinicio.

## Gate y dictamen

No cerrar mientras haya un gate aplicable `NOT_RUN`, `FAIL`, `REVIEW_REQUIRED` o mientras el contrato DB de binding permanezca bloqueado para el E2E docente. El cierre funcional requiere además `CLOSURE.md`, deuda y ledger manual conforme a la DoD única.
