# Matriz de contrato de seguridad — E2E real (2026-10-14)

Todas las filas `E2E-*` pasan por el **backend 8083 real** (Spring MVC + JPA, PEP ON, `-IdentityMode SharedLab`)
con el **PEP/PDP v0.1.6 real**, salvo E2E-11, donde el PEP es el stub de inyección de fallos
([`pep_fault_stub.py`](../../../infra/security-baseline-lab/scripts/pep_fault_stub.py)) y el backend sigue siendo
el real. Las filas `*-pre` consultan el PEP directamente para demostrar qué decidió el PDP para la misma identidad
y ruta (el backend no propaga el `X-Decision-Id` de una denegación). Salida completa:
[evidence/](evidence/). Commit del código ejecutado: `a33d8d0` (sin cambios Java en este work item).

**Capa** — cómo se atribuye: `X-Decision-Id` en la respuesta ⇒ el PDP permitió y la denegación vino después;
log `GlobalExceptionHandler … Handled application exception` con el mismo correlation ID ⇒ Application; sin
`X-Decision-Id` y sin ese log ⇒ PEP/PDP. Para los 401, cada token difiere del positivo en un único atributo
(verificado en `provisioning.json`), y el PEP no se consulta sin autenticación válida.

## Casos obligatorios

| Caso | Status | Code | Correlation ID | Decision ID | Capa | Registros | Resultado |
|---|---:|---|---|---|---|---:|---|
| 1 · E2E-01a horarios del docente FIX-PAG-300 | 200 | — | `01a23c96-53fe-4a0d-af06-4f72289c081c` | `8d06c31b-a834-46bb-a482-195ef4b9d446` | PEP/PDP allow + Spring + Application | 1 (= SQL) | PASS |
| 1 · E2E-01b estudiantes del grupo propio | 200 | — | `f5f3c9d8-be2a-4237-8ce0-316ebf23e6b7` | `7cd4238f-b138-4594-9b9e-bf69cb384032` | idem | 300 (= SQL) | PASS |
| 2 · E2E-02-pre PDP concede v2 del grupo ajeno | 204 | — | `sec-lab-de8bae5d-…` | `06f0c1b6-2f52-4e30-a63d-1e2678b8869b` | PEP/PDP directo | — | PASS |
| 2 · E2E-02a sesiones v2 grupo ajeno FIX-PAG-1000 | 403 | FORBIDDEN | `5a4b1edc-e7fe-4289-8f62-c6c090c2057f` | `191a1267-9627-4c67-96fa-c4a3c8eb4678` | **Application / InstitutionalScopePort** | 0 | PASS |
| 2 · E2E-02-pre PDP concede estudiantes del grupo ajeno | 204 | — | `sec-lab-cdc772a6-…` | `c77e72fa-c6e5-43ad-963f-f05c74198956` | PEP/PDP directo | — | PASS |
| 2 · E2E-02b estudiantes grupo ajeno FIX-PAG-1000 | 403 | FORBIDDEN | `774b9a38-81f0-43f3-99a4-0e78abec7fa4` | `a4dba4dc-b63b-4045-9bc4-ab35d65217b0` | **Application / InstitutionalScopePort** | 0 | PASS |
| 3 · E2E-03-pre PDP deniega sin asignación | 403 | ACCESS_DENIED | `sec-lab-9bf35a87-…` | `a4eca48c-8d1b-4891-b708-46a757f89e5f` | PEP/PDP directo | — | PASS |
| 3 · E2E-03 docente sin asignación PDP | 403 | FORBIDDEN | `6ed9f969-96ad-470d-84d5-55f089877cd2` | — | PEP/PDP deny | 0 | PASS |
| 4 · E2E-04a rol ESTUDIANTE en horarios | 403 | FORBIDDEN | `73dabc2c-e6be-4a32-9c6b-4d8147cab804` | `65faa38e-7eeb-408f-860a-fd91c477becb` | Spring AuthorizationFilter (rol) | 0 | PASS |
| 4 · E2E-04b rol ESTUDIANTE, titular de FIX-PAG-1000 | 403 | FORBIDDEN | `cd68d36d-e51e-466b-b23b-edb7f9a9d8ec` | `5e98a438-63ae-4f85-b756-f78e964b0dde` | Spring AuthorizationFilter (rol) | 0 | PASS |
| 5 · E2E-05 JWT sin `idUsuario` | 401 | UNAUTHORIZED | `701d692d-0bfb-48bc-8e82-6be3dd2ffb3a` | — | JwtDecoder (`RequiredUuidClaimValidator`) | — | PASS |
| 6 · E2E-06 `aud` sin `asistencias-api` | 401 | UNAUTHORIZED | `6ce0e770-de36-4186-a465-e52b641ad3b7` | — | JwtDecoder (`AudienceValidator`) | — | PASS |
| 7 · E2E-07 issuer institucional 8081 | 401 | UNAUTHORIZED | `510d9259-c34b-4470-b7c7-79a34c41abc9` | — | JwtDecoder (issuer/JWKS) | — | PASS |
| 8 · E2E-08 firma alterada | 401 | UNAUTHORIZED | `5cf0c6b1-af93-48bd-ba4a-3f2c65c7de72` | — | JwtDecoder (firma) | — | PASS |
| 9 · E2E-09 sin token | 401 | UNAUTHORIZED | `1ed3db10-baf6-4238-b322-920452c231d2` | — | Spring entry point | — | PASS |
| 10 · E2E-10a PEP detenido: horarios | 503 | ACCESS_DECISION_UNAVAILABLE | `dfeede1e-8af8-4f97-b0e9-14c867d6f5f4` | — | PEP client fail-closed | 0 | PASS |
| 10 · E2E-10b PEP detenido: SSE | 503 | ACCESS_DECISION_UNAVAILABLE | `cc77a0f3-0ba7-4b73-99da-5a8b6c86f352` | — | PEP client fail-closed | — | PASS |
| 10 · E2E-10c PEP detenido: sin token | 401 | UNAUTHORIZED | `946bf12a-bcef-4af2-831e-da60b72782f7` | — | Spring entry point | — | PASS |
| 10 · E2E-10d PEP detenido: actuator | 200 | — | `1c335303-c3bd-4ed3-8a11-f25b7fff82e7` | — | Ruta pública | — | PASS |
| 11 · E2E-11a 200 vacío sin decisión (patrón PEP-03) | 503 | ACCESS_DECISION_UNAVAILABLE | `0ccbac75-ce8a-49b8-8f10-8ba602f05815` | — | fail-closed | 0 | PASS |
| 11 · E2E-11b 204 sin `X-Decision-Id` | 503 | ACCESS_DECISION_UNAVAILABLE | `2a07fe4c-7c68-4bdb-85f9-42e7ae3a9e57` | — | fail-closed | 0 | PASS |
| 11 · E2E-11c 204 con `X-Decision-Id` inválido | 503 | ACCESS_DECISION_UNAVAILABLE | `a3a0e2df-3e49-4cef-b720-095c84c67fa4` | — | fail-closed | 0 | PASS |
| 11 · E2E-11d PEP 500 | 503 | ACCESS_DECISION_UNAVAILABLE | `4a579b6e-aaab-4e5a-8782-ee60d53a66ef` | — | fail-closed | 0 | PASS |
| 11 · E2E-11e PEP 302 | 503 | ACCESS_DECISION_UNAVAILABLE | `d6d9345e-74c3-418c-8da8-1989b09193bb` | — | fail-closed (no sigue redirect) | 0 | PASS |
| 11 · E2E-11f 200 con `X-Decision-Id` válido | 503 | ACCESS_DECISION_UNAVAILABLE | `e07bd56e-557d-483c-b232-598187721012` | — | fail-closed (ALLOW solo 204) | 0 | PASS |
| 11 · E2E-11g PEP > timeout | 503 | ACCESS_DECISION_UNAVAILABLE | `dc0d5061-466a-49db-b5c6-c84747db501a` | — | fail-closed | 0 | PASS |
| 12 · E2E-12 sesiones UTC v2 propias | 200 | — | `55944493-1427-40f5-a414-8518c1d624c4` | `f274fda9-da25-4841-abfc-b963823c514d` | allow completo | 6 (= SQL; 4 `UTC_V2`) | PASS |
| 13 · E2E-13a SSE autorizado (`grupoId` propio) | 200 `text/event-stream` | — | `cc375575-89d0-49eb-ab19-c007b10b28ff` | `2a393dec-0603-47d7-954a-117f484ab707` | allow completo | — | PASS |
| 13 · E2E-13b SSE `grupoId` ajeno (PDP permite la ruta) | **500** (contrato: 403) | — | `b5378cb3-f60c-4d67-b79b-5f3bece6ac1f` | `e54a4da9-6be8-4f54-b69d-6238d1e39bdd` | Application deniega; render del error falla | 0 (stream no abierto) | **FAIL** |
| 13 · E2E-13c SSE sin asignación PDP | 403 | FORBIDDEN | `078882b4-b567-41a8-98e3-d625c0851cbb` | — | PEP/PDP deny | — | PASS |
| 13 · E2E-13d SSE sin token | 401 | UNAUTHORIZED | `9ad03666-cbc0-49e2-9792-57faee202c68` | — | Spring entry point | — | PASS |
| 13 · E2E-13e SSE sin `grupoId` | 400 | — | `8a20cb9f-9210-4907-b6fa-6db822a95772` | `9ac77cbe-906c-489a-a771-72900af83f16` | Controller (`grupoId` obligatorio) | — | PASS |
| inf · E2E-14 UUID propio en mayúsculas | 403 | FORBIDDEN | `66756494-4ea1-499a-b6f1-1953ce55643c` | — | PEP/PDP deny (ruta no registrada) | — | PASS |
| inf · E2E-15 actuator | 200 | — | `30c6b79a-6e25-4a39-9804-35ca7bee4832` | — | Ruta pública | — | PASS |

Totales: `e2e` 23 (22 PASS, 1 FAIL), `e2e-pep-down` 4/4, `e2e-fault` 7/7. Datos de E2E-12 comparados
campo a campo con SQL: ids de sesión, grupo, `procedenciaTemporal` y `fechaHoraInicio/Fin` (`UTC_V2` = valor SQL + `Z`).

### E2E-13b — defecto preexistente (no del PEP)

`LocalSseRealtimeStreamGateway` lanza `ForbiddenException` (titularidad) y `GlobalExceptionHandler` registra
`status=403, code=FORBIDDEN`, pero su `ResponseEntity` JSON no es aceptable para `Accept: text/event-stream`
(lo que envía `EventSource`): `HttpMediaTypeNotAcceptableException` → **500**. El acceso queda denegado (sin
stream ni datos), pero viola el OpenAPI (`403 Forbidden`). El código implicado no difiere de `develop` `4082736`.
Se registra como FAIL propio y se deriva a una tarea separada; no se corrige aquí (fuera del alcance de seguridad
experimental y cambia el contrato de errores de todo el backend).

## PEP + PDP reales sin backend (`lab_matrix.py pep`)

13 PASS + PEP-03 `KNOWN_UPSTREAM_FAIL` (exit 2). ALLOW exige ahora 204 + `X-Decision-Id` válido: PEP-06, PEP-11 y
PEP-14 lo cumplen. PEP-03 (200 sin decisión cuando falta el Bearer) sigue siendo un fallo upstream; el backend no
reenvía peticiones sin Bearer y E2E-11a demuestra que, si ocurriera, respondería 503.

## Limitaciones de autorización PDP

- Rutas exactas por UUID: válidas para esta demostración, **no** para grupos dinámicos (plantillas `{x}` → 500 en
  v0.1.6). Cada grupo nuevo exigiría un recurso nuevo.
- La comparación de rutas es literal: el mismo grupo con UUID en mayúsculas se deniega (E2E-14, fail-closed).
- El PEP recibe `getRequestURI()` sin query string: en SSE el PDP autoriza `/api/v1/realtime/stream` para cualquier
  `grupoId`; la titularidad del gateway es la única barrera académica (E2E-13a/13b).
- `environment` no está ligado a la aplicación (PEP-14).

## `ACCESS_DECISION_UNAVAILABLE` (solo rama experimental)

Incorporado a [BACKEND_GOLDEN_PATH_CONTRACT §G](../../contracts/BACKEND_GOLDEN_PATH_CONTRACT.md) (fila experimental) y
al overlay [openapi/experimental/security-baseline-pep.overlay.yaml](../../contracts/openapi/experimental/security-baseline-pep.overlay.yaml)
(OpenAPI Overlay 1.0.0: `components.responses.AccessDecisionUnavailable` + `'503'` en toda operación con Bearer).
El OpenAPI canónico, su SHA-256 y sus tests de conformidad **no** cambian: describen el comportamiento por defecto
(flag OFF), donde el 503 no existe. Un primer intento de añadir el 503 al YAML canónico rompió
`OpenApiGoldenPathValidationTest` (SHA congelado) y `OpenApiEstudiantesDirectoryConformanceTest` (status exactos);
se revirtió en lugar de tocar esos gates. Requiere aprobación contractual antes de merge.
