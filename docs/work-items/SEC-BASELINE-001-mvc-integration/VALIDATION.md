# Validación

Código validado: commit `16621c7` sobre `83ab487` (rama `jose-valencia/experiment-security-baseline-mvc`).
Las filas de 2026-10-13/14 anteriores a `16621c7` validan la versión `a74b288` y se conservan como historia.

## Historia de ejecuciones

| Fecha | Comando/acción | Resultado | Alcance/limitación |
|---|---|---|---|
| 2026-10-13 | Inspección tag `v0.1.6` y assets publicados | PASS | Código/contratos leídos; PDFs solicitados ausentes |
| 2026-10-13 | `docker compose ... config --quiet` del laboratorio (copia sin versionar) | PASS, exit `0` | Validación estática; engine no accesible desde esa sesión |
| 2026-10-14 | Focales `SecurityBaselinePepFilterTest` 3/3, `SecurityBaselinePepClientTest` 2/2, `SecurityConfigTest` 12/12 sobre `a74b288` | PASS | Unitarios; sin proveedor real |
| 2026-10-14 | `.\mvnw.cmd -B -ntp verify` sobre `a74b288` | PASS, 1539/1539 | JaCoCo LINE 90.54 % / BRANCH 79.79 %. No detectó el bypass por doble registro (no había test de wiring) |
| 2026-10-14 | Publicación `a74b288`, luego `83ab487` (docs) | PASS | PR no creado (conector GitHub 403) |

## Esta sesión (2026-10-14, Docker Engine accesible)

| Comando/acción | Resultado | Evidencia/limitación |
|---|---|---|
| `docker version` / `context ls` / `compose version` | PASS | Engine 28.4.0, contexto `desktop-linux`, Compose v2.39.4. Se usó `--context desktop-linux` explícito |
| `docker compose ... -f infra/security-baseline-lab/compose.lab.yaml config --quiet` | PASS, exit `0` | Puertos solo `127.0.0.1`; volúmenes y red propios |
| `docker compose ... pull` | PASS | Digests en `LAB_RUNBOOK.md` |
| `up -d --wait keycloak` + `rotate-lab-client-secrets.ps1` | PASS | `security-baseline-bff` y `security-pep-internal` → `ROTATED_AND_VERIFIED`; `security-demo` deshabilitado |
| `up -d --wait` (6 servicios) | PASS | keycloak/surrealdb/redis/pdp/pep `healthy`; OPA `/health` y `/health?bundles=true` 200 desde la red |
| `GET 127.0.0.1:18080/actuator/health`, `GET 127.0.0.1:18081/actuator/health`, PEP `/health/readiness` | PASS | `{"status":"UP"}`; readiness PEP incluye el PDP |
| Resolución de `keycloak.localhost` desde PDP/PEP | FAIL → corregido | La prueba con curl vía `host-gateway` no fue concluyente (curl resuelve `*.localhost` a loopback sin DNS). Con alias DNS, el PDP falló el callback OIDC con `Connection refused: localhost/127.0.0.1:19090` (Netty trata `*.localhost` como loopback si no hay entrada en `/etc/hosts`). Corregido con IP fija `172.31.240.10` + `extra_hosts`; login BFF 200 |
| `provision_lab_pdp.py` (y re-ejecución) | PASS, idempotente | 3 usuarios sintéticos, app `8cdd5dcb-bd85-4170-a172-a7459bfcb2fb`, 3 recursos, rol, 2 concesiones, 1 asignación; recuentos estables en la segunda corrida |
| Recursos con plantilla `{grupoId}` | FAIL upstream | PDP v0.1.6 responde `500 INTERNAL_ERROR`; se usaron rutas exactas |
| `python -I lab_matrix.py pep` (PEP+PDP reales, sin backend) | 13/14 PASS | PEP-03 FAIL: sin Bearer el PEP responde 200 sin decisión (hallazgo upstream, ver `RISKS.md`) |
| Mutación: quitar `FilterRegistrationBean` deshabilitado | Detectada | 9 fallos + 1 error en `SecurityBaselinePepWiringTest`; con el wiring de `a74b288` el PEP se omite por completo |
| Mutación: aceptar cualquier 2xx | Detectada | Falla cliente y wiring |
| Backend 8083 primera matriz | 7/10 PASS | JWT institucional → 503: el PEP devolvió `501` al `Upgrade: h2c` del cliente JDK |
| Mutación: quitar `HTTP_1_1` | Detectada | `SecurityBaselinePepWiringTest` captura `Upgrade: h2c` |
| Backend 8083 (`16621c7`) `lab_matrix.py backend` | 10/10 PASS | Los correlation IDs aparecen en el log del PEP con el 401 real |
| `docker stop asistencias-security-lab-pep-1` + `lab_matrix.py backend-pep-down` + `docker start` | 4/4 PASS | 503 `ACCESS_DECISION_UNAVAILABLE`; sin token 401; actuator 200; PEP healthy tras reinicio |
| Focales `SecurityBaselinePep*Test` + `SecurityConfigTest` | PASS, 43/43 | Incluye 13 de wiring con PEP simulado a nivel HTTP |
| `.\mvnw.cmd -B -ntp verify` sobre `16621c7` | PASS, exit `0`, 1565/1565 | ArchUnit PASS; JaCoCo LINE 90.74 % / BRANCH 80.64 %; 03:41 min |
| `git diff --check`; escaneo de los 8 secretos de `lab.env` en 37 archivos | PASS | 0 coincidencias |
| WebFlux / JDBC nuevos | PASS | `pom.xml` sin cambios respecto a `develop`; Reactor preexistente solo en SSE local; sin `java.sql`/`JdbcTemplate` nuevos |
| Servicios ajenos | PASS | SQL Server, Keycloak 8081 (+DB), MinIO, ClamAV y observabilidad con `startedAt` 04:48Z previo a la sesión y `restarts=0` |

## No ejecutado

| Caso | Estado | Motivo |
|---|---|---|
| E2E académico 2xx (PEP permite + rol/ownership locales) | `BLOCKED_BY_IDENTITY_CONTRACT` | Ver `IDENTITY_MAPPING.md` |
| PDP detenido | `NOT_RUN` (real) | La autorización solo permite detener el contenedor `pep`; cubierto por mapeo 5xx/timeout simulado |
| Timeout del PEP real | `NOT_RUN` (real) | Cubierto por PEP simulado (`slow`) y unitario |
| `-Pintegration verify` | `NOT_RUN` | No cambia persistencia; sin escrituras en DB |
| CI remoto / Sonar | `NOT_RUN` | No hay PR |
