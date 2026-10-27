---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# VALIDATION — HU053/HU054/HU055: solicitudes de ingreso a un grupo

## Dictamen

PASS (local). Auditor: el mismo agente que implementó (sin independencia); la revisión independiente queda para el PR. Sin push hasta la confirmación de John.

## Comandos ejecutados

| Fecha/entorno/base | Comando | Exit code | Resultado |
|---|---|---|---|
| 2026-10-27, DB, SQL Server 2022 desechable (réplica del job `DB Quality Gate CI/CD`) | freeze Verify + deploy + fixture + `test_summary.ps1` | 0 | `FREEZE_EXIT=0`; `TEST_PASS:S-01..S-12`; `FAILED=0`; `SQL_ERROR_COUNT=0`; `DB GATE PASS` |
| 2026-10-27, backend | pruebas focales (RS, SI, RW, CD, CE, RG, CTD, CTE, CLS, catálogo, unicidad, arquitectura) | 0 | 78 pruebas OK |
| 2026-10-27, backend (1.ª corrida) | `./mvnw -B -ntp clean verify` | 1 | `JpaRepositoryArchitectureRulesTest`: el repositorio dependía de `java.sql.Timestamp`; corregido (se usa `java.util.Date`) |
| 2026-10-27, backend | `./mvnw -B -ntp clean verify` | 0 | `Tests run: 2588, Failures: 0, Errors: 0`; `All coverage checks have been met` (JaCoCo + ArchUnit) |
| 2026-10-27, frontend | `npm run test:ci` | 0 | `TOTAL: 1257 SUCCESS` (1241 + 16: SV-01..05, SE-01..05, TS-01..05, MK-01) |
| idem | cobertura de líneas en código nuevo (lcov × diff contra `df925f2`) | — | 145/154 = 94.2 % (umbral SonarCloud 80 %) |
| idem | `npm run coverage:realtime:check` / `ng build --configuration=production` | 0 / 0 | `PASSED` / bundle OK |

## Checks

| Nivel | Check | Resultado | Evidencia/motivo |
|---|---|---|---|
| Alcance | Decisiones D1, D2, D4 (D3/D5 descartados) | PASS | S-05..S-10, RS-01..03 |
| Esquema | Tabla nueva aprobada; sin tablas existentes modificadas; congelados intactos | PASS | `FREEZE_EXIT=0`; FKs solo desde la tabla nueva |
| Arquitectura | ArchUnit | PASS | tras corregir la dependencia a `java.sql` |
| Contratos | HTTP/DB | PASS | CTD/CTE, closure (lecturas y comando con columna extra excluidos) |
| Seguridad | Catálogo de acceso (4 filas nuevas) y ámbito en SQL | PASS | `AccessCatalogConsistencyTest`; S-04, S-09, S-10, S-11 |
| Tests | RED → GREEN, negativos | PASS | ver [TEST_PLAN.md](TEST_PLAN.md) |
| Coverage | JaCoCo / SonarCloud estimado | PASS | backend OK; frontend 94.2 % en código nuevo |
| Documentos | WI en 3 repos, matrices, índices, MV-008, TD-066 | PASS | ver CLOSURE |

## Integridad de RED

Commits RED: DB `c9c6b9a`, backend `9e357dc`, frontend `1fa63b0`. Ajuste técnico del stub de `SolicitudIngresoGrupoJpaRepositoryTest` documentado en el TEST_PLAN (sin cambiar aserciones).

## Bloqueantes y limitaciones

- Sin prueba automática de dos sesiones concurrentes (protección por `UPDLOCK, HOLDLOCK`); se valida en MV-008.
- El PDP del Security Baseline debe reaprovisionarse con las 4 rutas nuevas (Alejo).
- Avisos solo en el log hasta integrar el componente de notificaciones (TD-066).
- La rama está apilada sobre HU051 (ya mergeado en `develop`): antes del push se integra `origin/develop` y se revalida.
