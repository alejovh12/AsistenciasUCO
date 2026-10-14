# VALIDATION — SEC-BASELINE-003 (2026-10-14, Windows 11, JDK 25, Python 3.14, PowerShell 5.1)

Rama `jose-valencia/experiment-security-baseline-mvc`. Una prueba no ejecutada no es PASS. Horas en UTC; `~` = aproximada (sin marca de tiempo en la evidencia), el resto tomadas de logs, `checkedAt` o snapshots.

## Fase 0 — Preservación

| Comprobación | Resultado |
|---|---|
| `git fetch`; `origin/develop` = `72312d7` con `12f4fdd` (FIX-001) | PASS |
| CI de `72312d7` (push a `develop`): SonarCloud, Backend Quality Gate, ArchUnit, CodeQL, Trivy ×2 `success`; Dependency Review `skipped`; QG Sonar `develop` `OK` | PASS |
| Org `develop` `4082736` ancestro de personal; sin commits exclusivos en ramas compartidas | registrado, sin sincronizar ([AS_IS](AS_IS.md)) |
| Worktrees y cambios MAINT-003L del checkout estable intactos; sin reset/clean/stash | PASS |
| Keycloak 8081 arriba y sin cambios; backend 8080 no se arrancó | PASS |

## Fase 1 — FIX-001 en el experimento

| Hora | Comando | Exit | Resultado |
|---|---|---:|---|
| ~13:57 | `git merge --no-ff origin/develop` en `.w/sec-mvc` | 0 | merge `8b50343`; 0 archivos en común entre FIX-001 y SEC-BASELINE-001/002 (sin conflictos ni commits duplicados) |
| 13:58 | `.\mvnw.cmd -B -ntp -Dtest=RealtimeStreamErrorHttpTest,GlobalExceptionHandlerFrameworkErrorsTest,SecurityBaselinePepWiringTest,SecurityBaselinePepClientTest,SecurityConfigTest -Djacoco.skip=true test` | 0 | 42 tests, 0 fallos (4 + 7 + 13 + 6 + 12) |
| 14:02 (fin) | `.\mvnw.cmd -B -ntp verify` | 0 | **1571 tests** (1565 de SEC-BASELINE-002 + 6 de FIX-001), 0 fallos, 0 skipped; ArchUnit OK; JaCoCo **LINE 90,75 % / BRANCH 80,68 %** (≥ 80/70) |

Comprobaciones: `pom.xml` sin cambios respecto de `develop`; ninguna línea añadida en `src/main` con WebFlux,
`WebClient`, JDBC, `java.sql`, `@Entity`/`@Table`; `app.providers.security-baseline-pep.enabled=${SECURITY_BASELINE_PEP_ENABLED:false}`.
Este work item no cambia `src/**`, por lo que el `verify` de la fase 1 es el del código final.

## Fase 2 — SSE con PEP/PDP reales

Efecto previo de los scripts: `verify-academic-identity.ps1` solo `SELECT`; `provision_shared_identity_e2e.py` **no se
re-ejecutó** (el estado del laboratorio de 002 persistía; los tokens se emitieron sin cambios); `start-backend-pep-lab.ps1`
solo variables de proceso.

| Hora | Comando | Exit | Resultado |
|---|---|---:|---|
| 14:02 | `verify-academic-identity.ps1 -Mode Verify` / `-Mode Snapshot` | 0 | 3/3 `verified=True`; snapshot base |
| ~14:04 | `start-backend-pep-lab.ps1 -IdentityMode SharedLab` + `lab_matrix.py e2e` | 0 | **25/25** (E2E-13b 403 JSON) |
| ~14:05 | `docker stop …pep-1` + `lab_matrix.py e2e-pep-down` + `docker start` | 0 | 4/4; PEP readiness `UP` |
| ~14:06 | backend → `pep_fault_stub.py` 18089 + `lab_matrix.py e2e-fault` | 0 | 7/7 |
| 14:07 | jar con `-PepDisabled -Port 8084` + `lab_matrix.py stable` | 0 | 9/9 (comportamiento estable intacto) |
| 14:08 | `lab_matrix.py pep` | 2 | 13 PASS + PEP-03 `KNOWN_UPSTREAM_FAIL` |
| 14:08 | `verify-academic-identity.ps1 -Mode Snapshot` | 0 | idéntico; 0 escrituras |

## Fase 3 — Aislamiento SQL

| Hora | Comando | Exit | Resultado |
|---|---|---:|---|
| ~14:10 | `new-e2e-sqlserver.ps1` | 0 | contenedor/volumen/red/puerto exclusivos; 16.0.4265.3 |
| ~14:11 | `prepare-e2e-database.ps1` (1.er intento) | 1 | conflicto de collation en la huella (antes de cualquier backup); corregido |
| 14:12 | `prepare-e2e-database.ps1` | 0 | `COPY_ONLY`+`CHECKSUM`, `VERIFYONLY`, restore, huella idéntica, logins |
| ~14:13 | `test-e2e-sql-permissions.ps1` | 0 | 20/20 (luego 22/22 tras añadir RO-09/RO-10) |
| ~14:16 | 8083 con `-DatasourceEnv e2e-sql-runtime.env` + `lab_matrix.py e2e` | 0 | 25/25; sesiones SQL del PID solo en la instancia E2E |
| 14:22 | `assert-backend-isolated.ps1 -Port 8083` | 0 | `isolated:true`, `principalSessionsOfPid:0` |

## Fase 4 — Matriz académica

| Hora | Comando | Exit | Resultado |
|---|---|---:|---|
| ~14:23 | `verify-role-identities.ps1` | 0 | 9/9 identidades verificadas (solo `SELECT`, login readonly) |
| ~14:25 | `role_matrix_e2e.py provision` (1.er intento) | 1 | `size=200` no admitido por el PDP en asignaciones (400); corregido a 100 |
| ~14:26 | `role_matrix_e2e.py provision` | 0 | 9 usuarios, 27 recursos estáticos, claims 9/9 PASS |
| 14:27 | `role_matrix_e2e.py run` (1.a) | 1 | 123 PASS / 14 FAIL / 1 KNOWN_DEFECT: 11 por expectativas del arnés (501 AS-IS, v1 UTC), 2 por arnés (RC-03, CS-01) — ver [TEST_PLAN](TEST_PLAN.md#correcciones-del-arnés-durante-la-ejecución-no-de-expectativas-de-seguridad) |
| 14:31 | `role_matrix_e2e.py run` (2.a) | 2 | 151 PASS + 2 `KNOWN_DEFECT` |
| ~14:32 | `role_matrix_e2e.py revocation` | 2 | 3 PASS + 2 `KNOWN_DESIGN_LIMIT`; `lab-e2e-revocable` rehabilitado |
| ~14:34 | 8083 con `e2e-sql-readonly.env` (1.er intento) | 1 (arranque) | fail-fast de `SesionV2SchemaCompatibilityVerifier` (sin visibilidad de SP v2); `GRANT VIEW DEFINITION` en la instancia E2E |
| 14:38 | `lab_matrix.py e2e` + `role_matrix_e2e.py readonly-probe` (login readonly) | 0 / 0 | 25/25; escritura → 500 `DATABASE_OPERATION_ERROR`, 0 filas |
| 14:38 | Snapshot del principal | 0 | idéntico al inicial |
| 14:41 | 8083 + `docker stop …pdp-1` + `lab_matrix.py e2e-pdp-down` + `docker start` | 0 | 5/5; PDP `healthy`; `e2e` posterior 25/25 |
| 14:47 | `role_matrix_e2e.py run` (final, etiquetas de capa corregidas) | 2 | **153: 151 PASS + 2 `KNOWN_DEFECT`** (estable respecto de la 2.a) |
| 14:51 | Snapshot final del principal (`verify-academic-identity.ps1 -Mode Snapshot`) | 0 | idéntico al inicial: `userUpdates=0`, mismos conteos y checksums |

Arnés: `lab_matrix.py selftest` y `role_matrix_e2e.py selftest` OK; `ast.parse` de 7 scripts Python y parser de
PowerShell de 12 scripts/módulos sin errores.

## Secretos

`scan_secrets.py` sobre `infra/security-baseline-lab/` y este work item: **31 valores secretos comprobados, 0
coincidencias**; sin rutas personales (sustituidas por `%USERPROFILE%`). Tokens nunca impresos (solo resúmenes de claims).

## Estado al terminar

Backend 8083/8084 detenidos; stub detenido; laboratorio y `asistencias-e2e-sqlserver` arriba; Keycloak 8081 y SQL
principal sin cambios (salvo el `.bak` `COPY_ONLY` y su fila en `msdb`).

## Gates

| Gate | Estado |
|---|---|
| `verify` + ArchUnit + JaCoCo | PASS |
| FIX-001 integrado y E2E-13b 403 | PASS |
| DB E2E aislada + privilegio mínimo | PASS |
| Matriz E2E 100 % PASS | **NO** (5 no-PASS diagnosticados) |
| MFA, frontend/PKCE | NOT_RUN |
| CI remoto de la rama experimental | se registra en [HANDOFF](HANDOFF.md) tras el push |
