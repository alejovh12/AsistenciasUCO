# AS-IS — SEC-BASELINE-003 (línea base comprobada el 2026-10-14)

## Git

| Ref | SHA | Observación |
|---|---|---|
| `develop` personal (`origin`, alejovh12/AsistenciasUCO) | `72312d7` | Merge de PR #23 (FIX-001, `12f4fdd`) presente |
| `develop` local del checkout estable | `4082736` | 2 commits detrás de `origin/develop`; **no se actualizó** porque el checkout tiene cambios ajenos sin commit (MAINT-003L) |
| `develop` organización (AsistenciasUCO/AsistenciasUCO-Backend) | `4082736` | Ancestro estricto de `72312d7`: le faltan `12f4fdd` y `72312d7`; sin commits exclusivos |
| `jose-valencia/experiment-security-baseline-mvc` (personal) | `da4019d` → `8b50343` (merge) → `a53fe25` → cierre | Ver [VALIDATION](VALIDATION.md) |
| idem en la organización | `83ab487` | Ancestro de la rama personal (SEC-BASELINE-001 temprano) |
| `jose-valencia/maint-003l-pagination-e2e-local` | personal `015fc6c` / org `1959460` | Org es ancestro de personal (3 commits de documentación solo en personal) |

Ramas solo en la organización (sin equivalente personal; no se tocan): `feature/backend-sse-e2e-alignment`,
`feature/hu050-matricula-docente-jpa`, `jose-valencia/lb-002.2a-jpa-command-plan`, `test-integracion-seguridad`,
`work/lb-002.1c-b1-backend-as-is`, `work/lb-002.1c-td043-debt-consolidation`. Solo en personal:
`jose-valencia/fix-001-sse-error-content-negotiation`.

**Reconciliación propuesta (no ejecutada):** ninguna rama compartida tiene commits exclusivos en la organización, así
que `develop`, la rama experimental y MAINT-003L pueden avanzar por *fast-forward* mediante PR en el repositorio de la
organización (nunca `--force`). Requiere decisión del usuario y permisos sobre la organización.

## Worktrees (estado preservado, sin reset/clean/stash)

| Worktree | Rama / HEAD | Estado |
|---|---|---|
| checkout estable | `develop` `4082736` | Cambios sin commit de MAINT-003L (3 `M`, 4 `??`) — intactos |
| `.w/sec-mvc` | rama experimental | Limpio al empezar; trabajo de este work item |
| `.claude/worktrees/nifty-hawking-3937fd` | `fix-001-…` `12f4fdd` | Intacto |
| `.workspace/maint-003l-prep` | MAINT-003L `015fc6c` | Intacto |
| otros 9 (`.codex/…`, `.workspace/…`) | ramas MAINT/PR anteriores | Intactos |

## CI posterior al merge de FIX-001 (`72312d7`, push a `develop`)

Backend CI, Backend Security y Backend Deep Security Scan: **todos `success`** (SonarCloud, Backend Quality Gate,
ArchUnit, CodeQL, Trivy repositorio e imagen; Dependency Review `skipped` en push). Quality gate SonarCloud de
`develop`: `OK`. Consultado vía API pública de GitHub/SonarCloud.

## Servicios

| Servicio | Estado | Cambios en este work item |
|---|---|---|
| Keycloak institucional 8081 (`asistencias-uco-security-keycloak-1`, 26.7.1) | arriba | **ninguno** (solo emisión de tokens de las cuentas E2E existentes) |
| SQL Server principal 1433 (`sql_server_asistencias`, 2022 CU26 16.0.4265.3) | arriba | solo `SELECT` + un `BACKUP … COPY_ONLY` (ver [DB_ISOLATION](DB_ISOLATION.md)) |
| Laboratorio `asistencias-security-lab` (Keycloak 19090 26.7.2, PDP 18080, PEP 18081, OPA, SurrealDB, Redis; imágenes `davidalzate/*:0.1.6`) | arriba | identidades/recursos de laboratorio (ver [SECURITY_E2E_MATRIX](SECURITY_E2E_MATRIX.md#aprovisionamiento)); PEP y PDP detenidos y rearrancados una vez cada uno |
| Backend estable 8080 | no estaba en ejecución | ninguno |
| Backend experimental 8083 / 8084 | arrancado y detenido varias veces | detenido al terminar |
| SQL Server E2E 14333 (`asistencias-e2e-sqlserver`) | **nuevo** | ver [DB_ISOLATION](DB_ISOLATION.md) |

## Código relevante (sin cambios en este work item)

- `GlobalExceptionHandler#buildResponse` fija `application/json` (FIX-001): el 403/400 de SSE ya no depende del `Accept`.
- `LocalSseRealtimeStreamGateway#subscribe`: titularidad síncrona antes de construir el `Flux` (no abre suscripción).
- `SecurityConfig`: RBAC por rutas; `SecurityBaselinePepFilter` solo con `app.providers.security-baseline-pep.enabled=true`
  (por defecto `false` en `application.yml`).
- `SesionV2SchemaCompatibilityVerifier`: al arrancar exige `OBJECT_ID` de `usp_crear_sesion_v2`/`usp_actualizar_sesion_v2`.
- `ReporteAsistenciaController` / `GenerarReporteAsistenciaUseCaseImpl`: **no** reciben el usuario autenticado
  (origen del IDOR DL-03).
