# PLAN — SEC-BASELINE-002: identidad compartida de laboratorio y E2E Security Baseline + MVC

## Identidad y objetivo

- Fecha / autor o rol / base Git: 2026-10-14 / backend-team (arquitectura de seguridad) / rama
  experimental `jose-valencia/experiment-security-baseline-mvc` desde `a33d8d0` (sobre `develop` `4082736`).
- Objetivo: resolver experimentalmente `BLOCKED_BY_IDENTITY_CONTRACT` de
  [SEC-BASELINE-001](../SEC-BASELINE-001-mvc-integration/IDENTITY_MAPPING.md) con una identidad **de laboratorio**
  cuyo `idUsuario` tenga procedencia académica verificada en SQL Server, y demostrar el recorrido
  JWT → Spring Security → PEP → PDP → RBAC local → `InstitutionalScopePort` → SQL Server → HTTP.
- Criterios de aceptación: 13 casos obligatorios del [TEST_PLAN](TEST_PLAN.md) ejecutados a través del backend
  8083, sin relajar validaciones, sin escrituras en la DB y sin secretos en evidencias.
- Skills y fuentes: `uco-seguridad`, `uco-testing`, `uco-realtime`; AGENTS.md; SEC-BASELINE-001
  (`IDENTITY_MAPPING`, `SECURITY_CONTRACT_MATRIX`, `LAB_RUNBOOK`, `RISKS`, `DECISIONS`); OpenAPI canónico;
  MAINT-003L (fixture FIX-PAG).

## AS-IS y evidencia

| Hecho | Fuente | Evidencia y límites |
|---|---|---|
| El PDP confía solo en `http://keycloak.localhost:19090/realms/security-baseline` con `aud=security-baseline-pdp` | `infra/security-baseline-lab/compose.lab.yaml` | Real, SEC-BASELINE-001 |
| El backend valida issuer exacto, firma (JWKS por descubrimiento OIDC), timestamps, `aud` y UUID `idUsuario` | `KeycloakSecurityAdapterConfiguration#jwtDecoder` | Configurable solo por `KEYCLOAK_*`; sin cambios de código |
| El realm del laboratorio no emitía `idUsuario` ni `resource_access.asistencias-api` | Admin API 19090 (lectura, 2026-10-14) | Realm mínimo: sin scope `roles`, sin atributo en User Profile |
| `de000003-…-0001` es titular único de FIX-PAG-300 (`de000005-…-0001`) | `gestionasistenciadb`, SELECT | [evidence/academic-identity-verification.json](evidence/academic-identity-verification.json) |
| No existe copia E2E de la DB ni principal de solo lectura; el backend usa `sa` | `sys.databases`, `sys.database_principals`, `IS_SRVROLEMEMBER` | Aislamiento solo operacional (ver [RISKS](RISKS.md)) |
| El JDK 25 de Windows resuelve `*.localhost` a 127.0.0.1 | Prueba `InetAddress.getAllByName` | Keycloak 19090 publicado en 127.0.0.1 |

## TARGET

Laboratorio: una identidad reconocida por ambas capas sin mapear `sub → idUsuario`, sin token
fabricado y sin tocar el Keycloak institucional ni la DB. **No** es la identidad institucional final.

## Clase de cambio y alcance de rutas

- Change class: INFRASTRUCTURE (laboratorio) + CONTRACT_CHANGE (solo en rama experimental, `ACCESS_DECISION_UNAVAILABLE`).
- PRIMARY_VARIABLE: `SHARED_LAB_IDENTITY_E2E`.
- ALLOWED_PATHS: `infra/security-baseline-lab/**`, `docs/work-items/SEC-BASELINE-002-shared-identity-e2e/**`,
  `docs/work-items/SEC-BASELINE-001-mvc-integration/**` (referencias), `docs/contracts/BACKEND_GOLDEN_PATH_CONTRACT.md`
  (fila 503 experimental) y `docs/contracts/openapi/experimental/**` (overlay). El OpenAPI canónico no se toca.
- FORBIDDEN_PATHS: `src/main/**` (salvo defecto bloqueante con aprobación), frontend, `develop`, realm/bootstrap
  institucional (`infra/keycloak/**`), SQL Server (esquema, datos, logins), Keycloak 8081, backend 8080.
- CONTRACTS: `ApiErrorResponse`; `ACCESS_DECISION_UNAVAILABLE` (503) marcado experimental.
- PROVIDERS: Keycloak 26 del laboratorio (19090), PDP/PEP Security Baseline v0.1.6.
- EXTERNAL_ENVIRONMENT: Compose `asistencias-security-lab`; SQL Server local (solo SELECT).
- SECURITY_IMPACT: issuer de laboratorio solo en el proceso 8083; validaciones intactas.
- OBSERVABILITY_IMPACT: ninguno en código; correlación verificada backend ↔ PEP/stub.
- TEST_LEVEL_REQUIRED: E2E real + `mvnw verify` + selftest del arnés.
- ROLLBACK: ver [HANDOFF](HANDOFF.md#rollback).
- CONSUMERS: ninguno nuevo (frontend fuera de alcance).
- STOP_CONDITIONS: UUID no verificado en SQL; necesidad de desactivar una validación; escritura detectada en la DB;
  necesidad de modificar 8081/8080/DB.

## Alcance / No alcance

Alcance: fases 1-9 del encargo. No alcance: frontend, merge, `develop`, activación en ambientes compartidos,
política de identidad productiva, corrección del defecto SSE 500 (tarea separada).

## Archivos afectados

EXISTENTES modificados: `infra/security-baseline-lab/scripts/{lab_matrix.py,start-backend-pep-lab.ps1}`,
`docs/contracts/BACKEND_GOLDEN_PATH_CONTRACT.md`, `docs/work-items/SEC-BASELINE-001-mvc-integration/{IDENTITY_MAPPING,LAB_RUNBOOK}.md`.
NUEVOS: `docs/contracts/openapi/experimental/security-baseline-pep.overlay.yaml`, `infra/security-baseline-lab/scripts/{verify-academic-identity.ps1,provision_shared_identity_e2e.py,pep_fault_stub.py}`
y este work item.

## Riesgos, test plan, rollback, deuda

[RISKS](RISKS.md), [TEST_PLAN](TEST_PLAN.md), [HANDOFF](HANDOFF.md). Deuda: defecto SSE 500 (contrato 403) y
limitaciones upstream (PEP-03, plantillas `{x}`, `environment`).

## Definition of Ready

READY para laboratorio: fuentes verificadas, UUID verificado, contrato de validación del backend conocido.
NOT_READY para producción: requiere decisiones de Seguridad-UCO ([IDENTITY_DECISION](IDENTITY_DECISION.md#decisiones-que-requieren-aprobación-institucional)).
