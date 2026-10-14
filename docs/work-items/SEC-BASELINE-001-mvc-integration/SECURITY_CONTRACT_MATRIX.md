# Matriz de contrato de seguridad

Leyenda de evidencia: **REAL** = backend 8083 y/o PEP+PDP v0.1.6 del laboratorio; **SIM** = PEP
simulado a nivel HTTP con el filtro registrado realmente (`SecurityBaselinePepWiringTest`); **UNIT** =
unitario. Un SIM/UNIT no certifica al proveedor. Correlation IDs en `VALIDATION.md` / salida de `lab_matrix.py`.

## Backend MVC con flag ON (`16621c7`)

| Caso | Esperado | Observado | Evidencia | Estado |
|---|---|---|---|---|
| Sin token en ruta protegida | 401 | 401 `UNAUTHORIZED`, PEP no consultado | REAL BE-01, SIM | PASS |
| JWT malformado | 401 | 401 | REAL BE-02 | PASS |
| JWT con firma alterada | 401 | 401 | REAL BE-03 | PASS |
| Issuer incorrecto (token del laboratorio) | 401 | 401 (rechazo local) | REAL BE-04 | PASS |
| Audience incorrecta | 401 | 401 | `SecurityConfigTest` (UNIT/slice); no hay token institucional real con otra audiencia | PASS (no real) |
| JWT institucional válido, PDP no reconoce su issuer (Escenario A) | 401 | 401; el PEP registra 401 con el mismo correlation ID | REAL BE-05/06/09 | PASS |
| Token válido sin permiso PDP | 403 | 403 `FORBIDDEN`, handler no ejecutado | SIM; decisión real del PDP en PEP-07/08/09/10 | PASS (E2E backend bloqueado por identidad) |
| Token válido + permiso PDP + rol local correcto | 2xx | 2xx con `X-Decision-Id` | SIM; ALLOW real 204 en PEP-06/11 | `BLOCKED_BY_IDENTITY_CONTRACT` para E2E |
| PEP permite, rol local insuficiente | 403 | 403 de Spring, handler no ejecutado | SIM | PASS (no real) |
| PEP permite, grupo ajeno al docente | Denegación de Application (`InstitutionalScopePort`) | Sin cambio: el filtro no toca ownership | Código; sin E2E | `BLOCKED_BY_IDENTITY_CONTRACT` |
| PEP no responde (contenedor detenido) | 503 | 503 `ACCESS_DECISION_UNAVAILABLE` | REAL BE-PEPDOWN-01/02 | PASS |
| PDP no responde | 503 | — | Mapeo 5xx SIM | NOT_RUN real (solo se autorizó detener `pep`) |
| Timeout PEP | 503 | 503 | SIM (`slow`), UNIT | PASS (no real) |
| Respuesta anómala (5xx, 200, 204 sin decisión, decisión inválida, 3xx) | 503 fail-closed | 503, sin seguir redirect | SIM, UNIT | PASS |
| PEP responde 501 a `Upgrade: h2c` | — | Causa raíz real corregida con HTTP/1.1 | REAL (primera corrida) + SIM | Corregido |
| Sesión UTC v2 autorizada | 200 | — | — | `BLOCKED_BY_IDENTITY_CONTRACT` |
| Coordinador con directorio permitido | 200 | 401 (Escenario A) | REAL BE-06 | `BLOCKED_BY_IDENTITY_CONTRACT` |
| Docente intentando directorio institucional | 403 | — | Regla local sin cambios (`SecurityConfigTest`) | `BLOCKED_BY_IDENTITY_CONTRACT` para E2E |
| SSE sin autorización | Denegación | 401 | REAL BE-08 | PASS |
| SSE autorizado | Conexión | — | ALLOW real del PDP para `GET /api/v1/realtime/stream` (PEP-11); dispatch ASYNC hereda la decisión (SIM) | `BLOCKED_BY_IDENTITY_CONTRACT` para E2E |
| Rutas públicas (`/actuator/**`, webhook Azure, Swagger GET, `/error`) | Sin PEP | 200 sin consulta | REAL BE-07, BE-PEPDOWN-04, SIM | PASS |
| Preflight CORS | 200 sin PEP | 200 con `Access-Control-Allow-Origin` | REAL BE-10, SIM | PASS |
| Ruta fuera de prefijos de negocio autenticada | Exige decisión | 403 con PEP deny | SIM | PASS |
| Flag OFF | Sin filtro, sin cliente, sin registro | Sin beans | UNIT (`ApplicationContextRunner`) | PASS |

## PEP + PDP reales, sin backend (`lab_matrix.py pep`)

| Caso | Esperado | Observado | Estado |
|---|---|---|---|
| PEP-01 sin credencial de aplicación | 401 | 401 `INTEGRATION_TOKEN_INVALID` | PASS |
| PEP-02 credencial incorrecta | 401 | 401 `INTEGRATION_TOKEN_INVALID` | PASS |
| PEP-03 sin Bearer | 401 | **200 vacío, sin `X-Decision-Id`** | FAIL upstream (fail-open); mitigado en nuestro cliente |
| PEP-04 Bearer con firma alterada | 401 | 401 `TOKEN_INVALID` | PASS |
| PEP-05 JWT institucional (Escenario A) | 401 | 401 `TOKEN_INVALID` | PASS |
| PEP-06 usuario con rol sobre recurso concedido | 204 | 204 + `X-Decision-Id` | PASS |
| PEP-07 usuario sin asignación | 403 | 403 `ACCESS_DENIED` + decisión | PASS |
| PEP-08 recurso registrado no concedido | 403 | 403 | PASS |
| PEP-09 ruta no registrada | 403 | 403 | PASS |
| PEP-10 método distinto | 403 | 403 | PASS |
| PEP-11 / PEP-12 SSE concedido / sin asignación | 204 / 403 | 204 / 403 | PASS |
| PEP-13 `applicationName` ajeno con credencial propia | 401 | 401 | PASS |
| PEP-14 `environment=prod` | informativo | 204 | El contrato v0.1.6 no vincula `environment` a la aplicación (riesgo registrado) |
