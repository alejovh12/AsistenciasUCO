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
