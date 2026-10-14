# PLAN — SEC-BASELINE-003: certificación local del laboratorio y preparación de la migración institucional

## Identidad y objetivo

- Fecha / rol / base Git: 2026-10-14 / backend-team (arquitectura de seguridad) / rama experimental
  `jose-valencia/experiment-security-baseline-mvc` desde `da4019d` + merge de `develop` `72312d7` (FIX-001, PR #23).
- Objetivo: (1) incorporar FIX-001 al experimento; (2) certificar SSE con PEP/PDP reales; (3) eliminar el bloqueo
  de aislamiento SQL de SEC-BASELINE-002 con una DB E2E realmente aislada y logins de mínimo privilegio; (4) ejecutar
  la matriz académica por roles con el proveedor real; (5) inventariar deuda; (6) preparar el ADR de migración.
- Criterios de aceptación: casos del [TEST_PLAN](TEST_PLAN.md) ejecutados por HTTP real (no MockMvc) contra el
  backend 8083 y el PEP/PDP v0.1.6; escrituras solo sobre la DB aislada con guard previo; sin secretos en evidencias;
  `verify` verde sin pérdida de tests; flag PEP OFF por defecto.
- Skills y fuentes: `uco-seguridad`, `uco-testing`, `uco-realtime`; AGENTS.md; SEC-BASELINE-001/002; FIX-001;
  OpenAPI canónico (`LocalSessionDateTime`, `UtcInstant`, x-roles); `SecurityConfig`; `docs/security/runtime-security-provider-architecture.md`.

## Autorización recibida (del usuario, 2026-10-14)

Autorizado: auditar, ramas/worktrees aislados, corregir defectos experimentales, tests y documentación, Maven/Docker/HTTP,
SQL Server E2E aislado con usuario de solo lectura (sin afectar el principal), pruebas contra PEP/PDP/Keycloak del
laboratorio, commits/push de ramas experimentales y PR de revisión.

**No autorizado:** merge a `develop`, reemplazar el proveedor de seguridad de `develop`, apagar/eliminar Keycloak 8081,
borrar datos/volúmenes/contenedores, migraciones destructivas, desplegar en ambientes compartidos, modificar el frontend
estable, publicar secretos, debilitar JWT o permisos, fusionar sin revisión.

## Clase de cambio y alcance de rutas

- Change class: INFRASTRUCTURE (laboratorio) + DOCUMENTATION_ONLY. Sin cambios en `src/**` en este work item (el
  único cambio de `src/**` entra por el merge de FIX-001, ya revisado y fusionado en `develop`).
- PRIMARY_VARIABLE: evidencia E2E real del recorrido JWT → PEP/PDP → RBAC → titularidad → SQL aislado → HTTP.
- ALLOWED_PATHS: `infra/security-baseline-lab/**`, `docs/work-items/SEC-BASELINE-003-local-certification-migration-readiness/**`.
- FORBIDDEN_PATHS: `src/**` (salvo el merge FIX-001), `pom.xml`, `docs/contracts/openapi/openapi-golden-path.yaml`,
  `docs/baseline/TECHNICAL_DEBT.md` (la deuda se propone, ver [TECHNICAL_DEBT](TECHNICAL_DEBT.md)), `infra/keycloak/**`,
  frontend, `.env` del checkout estable.
- CONTRACTS: OpenAPI canónico sin cambios; overlay experimental 503 sin cambios.
- PROVIDERS: Keycloak 19090 + PEP 18081 + PDP 18080 (v0.1.6) del laboratorio; SQL Server E2E 14333 (nuevo).
- EXTERNAL_ENVIRONMENT: solo local (loopback). Keycloak 8081 y SQL principal 1433: solo lectura.
- SECURITY_IMPACT: ninguno sobre `develop`; el laboratorio gana identidades y recursos PDP exclusivos.
- OBSERVABILITY_IMPACT: ninguno en código; se usan `X-Correlation-Id`, `X-Decision-Id`, logs JSON y el gauge
  `realtime_subscribers_active`.
- TEST_LEVEL_REQUIRED: `verify` completo + E2E real + pruebas de permisos SQL.
- ROLLBACK: [ROLLBACK](ROLLBACK.md).
- CONSUMERS: ninguno (rama experimental).
- STOP_CONDITIONS: el guard de aislamiento falla; una escritura alcanzaría el SQL principal; un script cambiaría
  Keycloak 8081; un secreto aparece en una ruta versionada.

## Fases y resultado

| Fase | Resultado | Documento |
|---|---|---|
| 0 Línea base y protección | PASS | [AS_IS](AS_IS.md) |
| 1 FIX-001 en el experimento | PASS (merge `8b50343`, `verify` 1571/0) | [VALIDATION](VALIDATION.md) |
| 2 SSE con PEP/PDP reales | PASS (E2E-13b 500 → 403 JSON) | [SECURITY_E2E_MATRIX](SECURITY_E2E_MATRIX.md) |
| 3 Aislamiento SQL | PASS | [DB_ISOLATION](DB_ISOLATION.md) |
| 4 Matriz académica | PARCIAL: 2 defectos AS-IS, 2 límites de diseño, 1 upstream; casos NOT_RUN listados | [SECURITY_E2E_MATRIX](SECURITY_E2E_MATRIX.md) |
| 5 Deuda | Inventario priorizado | [TECHNICAL_DEBT](TECHNICAL_DEBT.md) |
| 6 Arquitectura de migración | ADR propuesto (no aprobado) | [MIGRATION_ADR](MIGRATION_ADR.md) |
| 7-8 Documentación y publicación | Commit + push de la rama experimental; PR en borrador | [HANDOFF](HANDOFF.md) |

## No alcance

Merge a `develop`; cambio del proveedor de seguridad de `develop`; corrección de defectos AS-IS de `develop`
(se derivan a tareas separadas, p. ej. el IDOR del reporte Excel); frontend; sincronización del repositorio de la
organización; MFA (NOT_RUN, ver [TEST_PLAN](TEST_PLAN.md#not_run)).
