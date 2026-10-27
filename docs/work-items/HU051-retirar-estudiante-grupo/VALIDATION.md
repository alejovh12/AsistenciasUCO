---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# VALIDATION — HU051: retiro de un estudiante del grupo

## Dictamen

PASS (local). Auditor: el mismo agente que implementó (sin independencia); la revisión independiente queda para el PR. Rama subida con autorización de John el 2026-10-27; CI remoto en los PR.

## Comandos ejecutados

| Fecha/entorno/base | Comando | Exit code | Resultado | Evidencia sanitizada |
|---|---|---|---|---|
| 2026-10-27, backend RED `e6d1d88` (worktree desechable) | `./mvnw -B -ntp -q test-compile` | 1 | paquetes `docente.retirarestudiantegrupo` inexistentes | ausencia del comportamiento |
| 2026-10-27, DB, SQL Server 2022 desechable (réplica del job `DB Quality Gate CI/CD`) | freeze Verify + `deploy_schema.ps1` + `prepare_lb004d_fixture.ps1` + `test_summary.ps1` | 0 | `FREEZE_EXIT=0`; `TEST_PASS:R-01..R-09`; `FAILED=0`; `DB GATE PASS` | log local |
| 2026-10-27, backend | pruebas focales (UC, RP, CT, CL, NT, `AccessCatalogConsistencyTest`, `JpaRepositoryBeanUniquenessTest`) | 0 | 32 pruebas OK | log local |
| 2026-10-27, backend | `./mvnw -B -ntp clean verify` | 0 | `Tests run: 2543, Failures: 0, Errors: 0`; `All coverage checks have been met` (JaCoCo + ArchUnit) | log local |
| 2026-10-27, frontend | `npm run test:ci` | 0 | `TOTAL: 1234 SUCCESS` (antes 1227; +7 = RT-01..RT-07); tras la revisión de SonarCloud del PR #17: `TOTAL: 1241 SUCCESS` (+RT-08..RT-14) y 100 % de cobertura en código nuevo; RT-10 detectó que el hub no propagaba "Retirar" (corregido en el frontend, commit `df925f2`) | log local |
| idem | `npm run coverage:realtime:check` / `ng build --configuration=production` | 0 / 0 | `PASSED` / bundle OK | log local |

## Checks

| Nivel | Check | Resultado | Evidencia/motivo |
|---|---|---|---|
| Alcance | Plan/criterios | PASS | R1-R6 cubiertas por R-01..R-09 y UC-01..UC-08 |
| Arquitectura | ArchUnit/dependencias | PASS | `NotificacionPort` en Application, adaptador y provider en Infrastructure; sin imports entre features |
| Contratos | HTTP/DB/seguridad | PASS | CT-01..04; resultset canónico del comando (closure); `HTTP_AS_IS_MATRIX` actualizado |
| Seguridad | Catálogo de acceso | PASS | `AccessCatalogConsistencyTest` con la fila nueva; `SecurityConfig` sin cambios |
| Seguridad | PII | PASS | NT-01: el log no contiene correos ni nombres; el correo no se expone por HTTP |
| Tests | RED → GREEN, negativos | PASS | ver [TEST_PLAN.md](TEST_PLAN.md) |
| Coverage | JaCoCo / gate realtime | PASS | `All coverage checks have been met`; frontend `PASSED` |
| Integración | DB real | PASS | SQL Server 2022, fixture con ROLLBACK |
| Build | mvn verify / ng build | PASS | ver comandos |
| Documentos | WI en DB/backend/frontend, índices, matrices, MV-008, LINEA_BASE | PASS | ver CLOSURE |

## Integridad de RED

Commits RED: DB `64c63fe`, backend `e6d1d88`, frontend `e5afbbb`. Ninguna prueba cambió después de RED.

## Bloqueantes y limitaciones

- En modo Security Baseline el PDP debe reaprovisionarse con la fila nueva del catálogo (`provision_asistencias.py`) — coordinar con Alejo.
- El aviso real (correo/WhatsApp/SMS) depende del componente de notificaciones aún no lanzado; hoy queda en el log (TD-066).
- Prueba manual en pantalla pendiente (MV-008).

## Evidencias remotas/manuales pendientes

GitHub Actions de los PR; MV-008 (docente retira, estudiante aparece inactivo, cupo liberado).
