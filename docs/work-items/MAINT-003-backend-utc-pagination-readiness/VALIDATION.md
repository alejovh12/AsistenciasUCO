---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-09
---

# MAINT-003 — VALIDATION

## GitHub Actions sobre fuente `c42129e504394f73768d32dd85f49473e248af3c`

| Workflow | Run | Resultado |
|---|---|---|
| Backend CI (Java25/Maven, JaCoCo, Sonar, Docker) | https://github.com/alejovh12/AsistenciasUCO/actions/runs/37903985248 | SUCCESS |
| Backend Security (CodeQL, Dependency Review) | https://github.com/alejovh12/AsistenciasUCO/actions/runs/37903985263 | SUCCESS |
| Backend Deep Security Scan (Trivy FS/Imagen, ArchUnit) | https://github.com/alejovh12/AsistenciasUCO/actions/runs/37903985240 | SUCCESS de jobs |

Backend CI job log: `HttpUtcInstantCodecTest` **7 tests, 0 failures, 0 errors, 0 skipped**. Total unit `mvn clean verify` **1453 tests, 0 failures, 0 errors, 0 skipped**, `BUILD SUCCESS`; SonarCloud Quality Gate SUCCESS. Importante: este conteo corresponde a la suite Surefire de la rama de PR20; no demuestra que se ejecutaron `-Pintegration` con SQL Server real.

## Límites de certificación

- `HttpUtcInstantCodec` es una clase **sin consumidores**, probada de manera independiente. HTTP v1 sigue devolviendo `LocalDateTime` sin offset.
- **No existe API v2 funcional en este PR.** Toda API v2 continúa CONTRACT_DECISION_REQUIRED. No publicar fechas `Z` para v1 sin migración aprobada.
- SQL Server `CoreViewQueriesJpaParityIT` sobre fixture >=11/multigrupo: NOT_RUN desde este PR. PR #18 tiene CI verde, pero esa integración no ha sido acreditada.
- Catálogo `CatalogJpaParityIT` contra SQL Server real: NOT_RUN desde este PR. PR #19 tiene CI verde, pero no acredita DB real.
- Auditoría de historiales `dbo.Sesion`: NOT_RUN; semántica del histórico por confirmar.
- Roundtrip sesión POST/GET/PATCH/GET SQL UTC, roles reales, navegador Berlin/London/Bogota: NOT_RUN.
- Migraciones y despliegue real: NO MODIFICADOS; pruebas E2E de UI pertenecen a fase frontend posterior.

**Resultado actual:** `PREPARATION_PASS / BACKEND_READY_FOR_FRONTEND = NO`. Solo firmar la entrega tras contrato UTC, tests SQL reales, autorización de owner/usuario y regresión integral.

## Evidencia adicional

Comandos de SQL y Maven, plan de pruebas y prompt de ejecución manual se encuentran en `PLAN.md`, `TEST_PLAN.md`, `BACKEND_TO_FRONTEND_HANDOFF.md` y `PROMPT_CODEX_BACKEND.md`. Nunca reportar `PASS` por ausencia de fallos en tests que no se ejecutaron.

## Ejecución independiente posterior — 2026-10-09

Esta sección reemplaza los `NOT_RUN` anteriores únicamente donde existe evidencia nueva. El detalle íntegro, SHAs, contrato y pendientes está en [BACKEND_READY_FOR_FRONTEND.md](BACKEND_READY_FOR_FRONTEND.md).

| Alcance | Resultado comprobado |
|---|---|
| PR18 unit | Java 25, 1452 tests, 0 failures/errors/skips, JaCoCo 92.53 % line / 80.97 % branch |
| PR18 SQL real | 192 IT, 0 failures/errors/skips; `CoreViewQueriesJpaParityIT` 7/0/0/0 y filtro académico 3/0/0/0 |
| Paginación directa/HTTP | clone 11 identidades/12 contextos/1 multigrupo; páginas 5/5/1, 11 distintos, 3 páginas; nueve filtros 200 con valores válidos; límites inválidos 400 |
| JWT real | Keycloak validado; anónimo 401, DOCENTE 403, ADMINISTRADOR 200. COORDINADOR/ESTUDIANTE reales NOT_RUN |
| PR19 unit | Java 25, 1447 tests, 0 failures/errors/skips, JaCoCo 92.50 % line / 80.70 % branch |
| PR19 SQL real | 191 IT, 0 failures/errors/skips; `CatalogJpaParityIT` 11/0/0/0; fixtures residuales 0 |
| PR20 unit | 1453 tests, codec 7, 0 failures/errors/skips, JaCoCo 90.26 % line / 80.25 % branch |
| Históricos | 3 sesiones caracterizadas; `HISTORICAL_TZ_UNDETERMINED`, sin escritura ni conversión |
| UTC HTTP/SQL | v2 no aprobada/no implementada: POST/GET/PATCH, SQL UTC y DST end-to-end continúan NOT_RUN |
| Calidad remota | 9/9 check-runs SUCCESS para cada SHA final de PR18, PR19 y PR20; CLI locales Sonar/CodeQL/Trivy no instalados |

Backup `COPY_ONLY` con checksum fue verificado antes de restaurar `gestionasistenciadb_fase1`. La base fuente quedó en 3 identidades, 3 contextos, 24 parámetros y 3 sesiones antes/después. No se ejecutó merge, push, DDL ni migración.

### Ledger de comandos y exit codes

Fecha de ejecución: 2026-10-09, zona `America/Bogota`. Los valores de conexión y tokens se inyectaron desde variables locales y no se imprimieron.

| Worktree/entorno | Comando o acción equivalente | Exit | Observación |
|---|---|---:|---|
| PR18 `39e34c49` | `.\mvnw.cmd -B -ntp clean verify` | 0 | 1452 unit, Java 25 |
| PR18 `39e34c49` + clone SQL | `.\mvnw.cmd -B -ntp -Pintegration verify` | 0 | 192 IT, 0 skips |
| PR19 `3985b0ac` | `.\mvnw.cmd -B -ntp clean verify` | 0 | 1447 unit, Java 25 |
| PR19 `3985b0ac` + clone SQL | `.\mvnw.cmd -B -ntp clean -Pintegration verify` | 0 | 191 IT, 0 skips; corrida válida fuera del sandbox |
| PR20 `c83442d5` | `.\mvnw.cmd -B -ntp clean verify` | 0 | 1453 unit; codec 7 |
| SQL Server clone | `pagination_readonly_diagnostics.sql` | 0 | páginas, nueve filtros y multigrupo, solo lectura |
| SQL Server clone | `utc_schema_probe.sql` + `utc_hypothesis_diagnostics.sql` | 0 final | primer borrador de hipótesis falló al compilar (Msg 130); corregido antes de obtener evidencia |
| SQL Server source + clone | `post_test_invariants.sql` | 0 | source intacta; cleanup catálogo 0 residuales |
| Keycloak local | `validate-keycloak.ps1` | 0 final | primer intento sandbox no tuvo socket; repetición autorizada: 0 fallos críticos, warnings legacy |
| Keycloak local | `seed-e2e-users.ps1` | 0 | tokens de ejemplo válidos; password no modificada por política local |
| Backend PR18 + clone + Keycloak | requests HTTP locales sanitizados | 0 | 401/403/200, páginas, filtros y 400 inválidos |
| Diffs | `git diff --check` | PR18 0; PR19 0; PR20 1 | PR20: whitespace final en skill, no fallo runtime |

Intentos PR19 con exit 1 antes de tests: ACL de `target` en worktree gestionado y pipe de attach de Mockito bloqueado por sandbox. No se usaron como evidencia; la repetición limpia posterior es la reportada.

**Resultado actualizado:** `PAGINATION_AND_CACHE_REAL_SQL_PASS / UTC_CONTRACT_BLOCKED / BACKEND_READY_FOR_FRONTEND = NO`.
