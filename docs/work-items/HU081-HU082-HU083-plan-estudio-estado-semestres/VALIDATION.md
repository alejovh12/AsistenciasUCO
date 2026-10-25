---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-24
---

# VALIDATION — HU081 / HU082 / HU083

Rama `feature/HU081-HU082-HU083-plan-estudio-estado-semestres` en DB, Backend y Frontend, desde `develop`. Validación local del 2026-10-24 (Windows 10, Java 25.0.5, Node 24.19, Docker 28.4). Sin push en el momento de esta validación.

## DB

| Comando | Resultado |
|---|---|
| `scripts/freeze-manifest.ps1 -Mode Verify` | exit 0 |
| `deploy_schema.ps1` + `test/prepare_lb004d_fixture.ps1` + `test_summary.ps1` sobre contenedor SQL Server 2022 limpio y desechable (réplica del job `DB Quality Gate CI/CD`) | `FAILED=0`, `DB GATE PASS`, exit 0 |
| `test_usp_plan_estudio_estado_semestres.sql` | `TEST_PASS` T-081-01, T-081-02, T-082-01, T-082-02, T-082-03, T-083-01, T-083-02 |

## Backend

| Comando | Resultado |
|---|---|
| `mvnw -B -ntp clean verify` | `Tests run: 2065, Failures: 0, Errors: 0, Skipped: 0`; `All coverage checks have been met`; `BUILD SUCCESS` |

No se ejecutaron localmente el build de la imagen Docker, Sonar ni CodeQL (los corre GitHub Actions en el PR).

## Frontend

| Comando | Resultado |
|---|---|
| `npm run test:ci` | `TOTAL: 1046 SUCCESS` (líneas 90.2 %, ramas 72.6 %) |
| `npm run coverage:realtime:check` | líneas 95.06 % (≥ 90 %), ramas 84.52 % (≥ 80 %) |
| `npm run build -- --configuration=production` | exit 0 |

## Limitaciones

- E2E con usuario real no ejecutado: el realm local no tiene usuarios de prueba sembrados y no se usan credenciales reales. Se valida con las pruebas de SP, use case, controlador, repositorio y componentes; queda pendiente la prueba manual en la UI con un coordinador.
- `CoordinadorPortalControllerTest.unavailableEndpointsRemainExplicitlyUnavailable` se actualizó (decisión D6 del PLAN): los tres endpoints dejan de ser 501 por este work item.
