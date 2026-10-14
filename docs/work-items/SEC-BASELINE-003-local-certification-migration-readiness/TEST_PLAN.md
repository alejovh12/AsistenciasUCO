# TEST_PLAN — SEC-BASELINE-003

Todas las pruebas E2E son **HTTP real** contra el backend 8083/8084 (jar de la rama experimental) y el PEP/PDP v0.1.6
del laboratorio; no MockMvc. Las expectativas se derivan del OpenAPI canónico, de `SecurityConfig` y de la
política documentada en `docs/security/runtime-security-provider-architecture.md`. Cuando el contrato no cubre un
endpoint, la expectativa se toma del código AS-IS **solo** para distinguir "no implementado" (501 por
`FeatureUnavailableException`) de un fallo; nunca para aceptar un acceso indebido.

## Integración requerida y ambiente

| Componente | Ambiente | Requisito |
|---|---|---|
| IdP | Keycloak 19090 realm `security-baseline` | Identidades `lab-e2e-*` asociadas a `idUsuario` verificado en SQL (< 24 h) |
| PEP/PDP | 18081/18080 v0.1.6 | Rol `MATRIZ_ROLES_E2E_LAB` + recursos exactos (plantillas `{x}` no soportadas) |
| Backend | 8083 `-IdentityMode SharedLab` (PEP ON); 8084 `-PepDisabled` (identidad institucional 8081) | jar de `8b50343`+ |
| DB | SQL E2E 14333 `gestionasistenciadb_e2e` (escrituras) / principal 1433 (solo lecturas de fase 2) | Guard `assert-backend-isolated.ps1` antes de toda escritura |

## Behavioral matrix

| Familia | Casos | Observable | Implementación equivocada que detecta |
|---|---|---|---|
| FIX-001 en laboratorio | E2E-13b/e/f | 403/400 con `Content-Type: application/json` y `code` contractual; gauge sin cambios | handler que negocia con `Accept: text/event-stream` (500 / 400 vacío) |
| SSE real | E2E-13a…g, SSE-* | 200 `text/event-stream` solo para el docente titular; 403 para ajeno/estudiante/coordinador; 401 sin token | suscripción abierta antes de validar titularidad; filtrado solo en memoria |
| Fail-closed | E2E-10*, E2E-11*, E2E-16* | 503 `ACCESS_DECISION_UNAVAILABLE` sin datos ni `X-Decision-Id` | PEP caído/PDP caído/2xx sin decisión/redirect/timeout tratados como ALLOW |
| Comportamiento estable | ST-01…09 | Mismo jar, flag OFF: sin `X-Decision-Id`, status/códigos de `develop` | el flag cambia el comportamiento por defecto |
| Identidad/JWT | provisioning `claims[*]`, E2E-05…09 | RS256, `kid` en JWKS, issuer exacto, `aud` ⊇ `asistencias-api`, `idUsuario` ≠ `sub`, TTL ≤ 300 s; 401 si falta cualquiera | validadores relajados |
| RBAC por rol | RB-* (14 rutas × 7 identidades + no-grant) | 200/501 a los roles permitidos, 403 `FORBIDDEN` con `X-Decision-Id` (PDP permitió, Spring denegó) al resto | regla RBAC ausente o demasiado amplia |
| PDP sin concesión | E2E-03, E2E-13c, RB-pdp-nogrant | 403 sin `X-Decision-Id` | PEP que deja pasar recursos no concedidos |
| Titularidad | E2E-02*, WR-11/12, AS-02, RC-04, DL-03, DL-08 | 403 (o 404 en soportes, contrato LB-004) con el PDP permitiendo | ownership ausente en Application/DB |
| Volumen | VOL-300/1000/5000, VOL-DIR-* | conteos = SQL; paginación `totalItems=5000`, `totalPages=50`, última página vacía | truncado, paginación incorrecta |
| Sesiones UTC | WR-01…10 | v2 normaliza offsets a UTC `Z`; v1 expone el mismo instante sin offset (`x-persistence-semantics: UTC`); escritura v1 ⇒ v2 `INDETERMINADA`; PUT v2 inexistente | doble conversión, pérdida de procedencia |
| Asistencias / reclamos | AS-*, RC-* | lote 201; revisión 202; aprobación por titular 200 y estado persistido | reclamo de otro rol / resolución por docente ajeno |
| DELETE | DEL-01…05 | 501 `FEATURE_UNAVAILABLE` tras pasar autorización; 403 por rol; matrícula intacta | borrado efectivo o sin autorización |
| Descargas | DL-01…08 | xlsx al titular/coordinador; soporte propio 200 con el mismo contenido; ajeno 404 | IDOR |
| CORS / CSRF | CO-*, CS-* | preflight con ACAO solo para origen permitido; POST con cookies sin Bearer = 401 (no CSRF 403); sin `Set-Cookie`; 0 filas | autenticación por cookie, origen comodín |
| Revocación | REV-01…05 | registra el comportamiento real de tokens tras logout/deshabilitar | — (límite de diseño; nunca PASS) |
| Privilegio mínimo SQL | RO-*, RT-*, ISO-01, FIX-01, RO-HTTP-* | `SELECT` vistas permitido; DML/DDL/EXEC denegados (229/262); escritura vía API falla en SQL sin filas | login con permisos excesivos o `sa` |

Veredictos: `PASS`; `FAIL`; `KNOWN_UPSTREAM_FAIL` (proveedor); `KNOWN_DEFECT` (defecto AS-IS de AsistenciasUCO ya
diagnosticado); `KNOWN_DESIGN_LIMIT` (límite arquitectónico). Ninguno distinto de `PASS` cuenta como PASS.

## Correcciones del arnés durante la ejecución (no de expectativas de seguridad)

| Caso | Primera ejecución | Causa | Corrección |
|---|---|---|---|
| RB-* admin/coord/perfil | 501 | Endpoints `FeatureUnavailableException` en el AS-IS (verificado en código) | Expectativa 501 para roles permitidos + rutas implementadas adicionales (`/admin/decanos`, `/coordinador/periodos-academicos`) |
| WR-05 | 13:00 en v1 | Expectativa mal derivada: el contrato `LocalSessionDateTime` declara `x-persistence-semantics: UTC` | Se espera el instante UTC sin offset |
| RC-03 | 403 | El arnés concedía la ruta en el PDP después de llamarla | Concesión antes de la llamada |
| CS-01 | 403 | Mezclaba origen no permitido (CORS) con CSRF | Separado en CS-01 (sin Origin → 401) y CS-01b (origen no permitido → 403 CORS) |

## NOT_RUN

| Caso | Motivo | Preparación requerida |
|---|---|---|
| MFA administrativo | El laboratorio no tiene política MFA verificable para el PDP/Keycloak de administración | Política institucional y configuración en Keycloak/PDP |
| Frontend Angular / BFF / Authorization Code + PKCE | Fuera de alcance (frontend estable no se modifica) | Work item frontend |
| `POST /api/v1/realtime/emit` y entrega cruzada de eventos | Utilidad diagnóstica fuera del OpenAPI; su 500 por `ResponseStatusException` está documentado en FIX-001. La no-entrega a grupos ajenos se demuestra porque no se abre suscripción (gauge + MockMvc FIX-001) | Decisión de retirar o contratar `/emit` |
| PDP con plantillas `{grupoId}` | Ya evidenciado en SEC-BASELINE-001 (500); no se repitió para no crear recursos inválidos | Corrección upstream |
| Timeout real del PDP | Solo PDP detenido (E2E-16) y timeout del PEP con stub (E2E-11g) | Inyección de latencia en la red del laboratorio |
| Escrituras de ADMINISTRADOR/COORDINADOR (crear grupo, docentes, etc.) | La mayoría declara `FEATURE_UNAVAILABLE`; las implementadas no están en el OpenAPI canónico | Contrato funcional |
| ~70 de ~110 operaciones | Matriz representativa (≈37 operaciones distintas, todas las del Golden Path de lectura/escritura de sesiones y asistencias) | Ampliar la matriz por contrato |
