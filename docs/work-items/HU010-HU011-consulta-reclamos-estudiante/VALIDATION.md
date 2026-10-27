---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-26
---

# VALIDATION — HU010/HU011 (consulta): Mis Reclamos del estudiante

## Dictamen

PASS (local, revalidado tras integrar `develop` el 2026-10-27). Auditor: el mismo agente que implementó (sin independencia); la revisión independiente queda para el PR. Ramas subidas con autorización de John; CI remoto en los PR.

## Comandos ejecutados

| Fecha/entorno/base | Comando | Exit code | Resultado | Evidencia sanitizada |
|---|---|---|---|---|
| 2026-10-26, Windows, backend RED `c16cb54` (worktree desechable) | `./mvnw -B -ntp -q test-compile` | 1 | RED: `package ...estudiante.consultarreclamos.primaryports does not exist` | fallo por ausencia del comportamiento |
| 2026-10-26, backend GREEN | `./mvnw -B -ntp clean verify` | 0 | `Tests run: 2159, Failures: 0, Errors: 0, Skipped: 0`; `All coverage checks have been met`; `BUILD SUCCESS` | log local |
| 2026-10-26, DB, SQL Server 2022 desechable (réplica del job `DB Quality Gate CI/CD`) | `scripts/freeze-manifest.ps1 -Mode Verify` | 0 | `FREEZE_EXIT=0` (sin objetos congelados tocados) | log local |
| idem | `deploy_schema.ps1` + `test/prepare_lb004d_fixture.ps1` + `test_summary.ps1` | 0 | `TEST_PASS:TEST_CONSULTAS_RECLAMOS_ESTUDIANTE`, `FAILED=0`, `DB GATE PASS` | log local |
| 2026-10-26, frontend | `npm run test:ci` | 0 | `TOTAL: 1093 SUCCESS` (antes 1085; +8 = SC-01..SC-08) | log local |
| idem | `npm run coverage:realtime:check` | 0 | líneas 95.06 %, ramas 84.52 %, `PASSED` | log local |
| idem | `npx ng build --configuration=production` | 0 | `Application bundle generation complete` | log local |
| 2026-10-27, tras rebase sobre `develop` con Security Baseline 0.1.7 | backend `clean verify` / DB gate / frontend `test:ci` + coverage + build | 0 | backend 2318 tests; DB `FAILED=0`; frontend 1148 tests | conteo JPA 41→42; `GET /estudiante/reclamos` ya estaba en `infra/security-baseline/access-catalog.json` |
| 2026-10-27, tras `git merge origin/develop` (indicadores: DB #50, backend #46, frontend #13) | backend `./mvnw -B -ntp clean verify` | 0 | `Tests run: 2517, Failures: 0, Errors: 0`; `All coverage checks have been met` (JaCoCo + ArchUnit) | conteo JPA 44 (develop) → 45 |
| idem | DB: freeze Verify + deploy + fixture + `test_summary.ps1` | 0 | `FREEZE_EXIT=0`, `TEST_PASS:TEST_CONSULTAS_RECLAMOS_ESTUDIANTE`, `FAILED=0`, `DB GATE PASS` | exclusiones READ_QUERY_RESULTSET de ambos lados en `test_contract_closure.sql` |
| idem | frontend `npm run test:ci` + `coverage:realtime:check` + `ng build --configuration=production` | 0 | `TOTAL: 1227 SUCCESS`, coverage `PASSED`, build OK | sin conflictos |

## Checks

| Nivel | Check | Resultado | Evidencia/motivo |
|---|---|---|---|
| Alcance | Plan/criterios | PASS | Solo `GET /estudiante/reclamos`; POST/DELETE siguen 501 (test `endpoints_no_implementados_lanzan_featureUnavailable`) |
| Arquitectura | ArchUnit/dependencias | PASS | suite `architecture.*` verde; sin imports entre features (no se reutilizan clases del feature docente) |
| Contratos | HTTP/DB | PASS | CT-01..03; E-08a mismas columnas que R2; `HTTP_AS_IS_MATRIX` actualizado |
| Contratos | LB-004D intacto | PASS | sin cambios en `/asistencias/revisiones`, `/archivos/*`, endpoints docente, MinIO/ClamAV, `soporteArchivoId`, correlación/idempotencia |
| Tests | RED → GREEN, negativos | PASS | RED registrado en [TEST_PLAN.md](TEST_PLAN.md); negativos UC-01/02/05, E-04/05/08b, SC-04/06/07 |
| Coverage | JaCoCo / gate realtime | PASS | `All coverage checks have been met`; frontend `PASSED` |
| Integración | DB real | PASS | SQL Server 2022 real, fixture con ROLLBACK, sin skips |
| Build | mvn verify / ng build | PASS | ver comandos |
| Seguridad | RBAC/ownership | PASS | Application exige estudiante institucional (403) y SQL filtra por `Estudiante.usuario`; sin cambios en `SecurityConfig` |
| Seguridad | Secretos | PASS | no se leyó ni escribió ningún secreto |
| Documentos | PLAN/CONTRACT/TEST_PLAN/CLOSURE/CONTRACT_MATRIX + WI en DB y frontend | PASS | este directorio y los homónimos |

## Integridad de RED

Commits RED: backend `c16cb54`, DB `e40edcf`, frontend `f01f953`. Ninguna prueba cambió después de RED (`git diff <RED> -- <tests>` vacío).

## Bloqueantes y limitaciones

- Docker Desktop tenía el motor colgado (HTTP 500); lo reinicié localmente antes de la réplica del gate DB.
- Prueba manual en la UI pendiente (MV-008, en espera del componente de seguridad).

## Evidencias remotas/manuales pendientes

- GitHub Actions de los 3 PR hacia `develop`.
- MV-008: estudiante real ve sus solicitudes y descarga su soporte; otro estudiante no las ve.
