# Matriz E2E de seguridad — SEC-BASELINE-003 (2026-10-14)

Todas las filas son peticiones HTTP reales. Backend: jar de la rama experimental (`8b50343`+, FIX-001 incluido) en
8083 con PEP ON e identidad `SharedLab`, o en 8084 con PEP OFF (filas `ST-*`). PEP/PDP: v0.1.6 reales, salvo
`E2E-11*` (stub de fallos). DB: SQL E2E aislado `gestionasistenciadb_e2e` (login `asistencias_e2e_runtime`) para la
matriz académica, revocación y PDP caído; principal en solo lectura para la repetición de SEC-BASELINE-002 y `ST-*`.
Evidencia completa (JSON por línea, sin tokens): [evidence/](evidence/).

**Atribución de capa:** `X-Decision-Id` en la respuesta ⇒ el PDP permitió y la denegación vino después (Spring o
Application); sin `X-Decision-Id` en un 403 ⇒ PEP/PDP; `GlobalExceptionHandler … Handled application exception` con el
mismo correlation ID ⇒ Application (comprobado por el arnés en `E2E-*`).

## Resumen

| Suite | Casos | PASS | No PASS | Detalle |
|---|---:|---:|---:|---|
| `lab_matrix.py e2e` (principal, lectura) | 25 | 25 | 0 | incluye E2E-13b/e/f/g |
| `lab_matrix.py e2e` (DB aislada, login runtime) | 25 | 25 | 0 | |
| `lab_matrix.py e2e` (DB aislada, login **solo lectura**) | 25 | 25 | 0 | |
| `lab_matrix.py e2e-pep-down` | 4 | 4 | 0 | |
| `lab_matrix.py e2e-pdp-down` | 5 | 5 | 0 | nuevo en 003 |
| `lab_matrix.py e2e-fault` (stub) | 7 | 7 | 0 | |
| `lab_matrix.py stable` (PEP OFF) | 9 | 9 | 0 | nuevo en 003 |
| `lab_matrix.py pep` (directo) | 14 | 13 | 1 | PEP-03 `KNOWN_UPSTREAM_FAIL` |
| `role_matrix_e2e.py run` | 153 | 151 | 2 | DL-03 y RB-admin decano `KNOWN_DEFECT` |
| `role_matrix_e2e.py revocation` | 5 | 3 | 2 | REV-02/04 `KNOWN_DESIGN_LIMIT` |
| `role_matrix_e2e.py readonly-probe` | 2 | 2 | 0 | |
| `test-e2e-sql-permissions.ps1` | 22 | 22 | 0 | ver [DB_ISOLATION](DB_ISOLATION.md) |
| **Total** | **296** | **291** | **5** | **no es 100 % PASS** |

### Respuesta a FIX-001 / E2E-13b

| Caso | SEC-BASELINE-002 (`a33d8d0`) | SEC-BASELINE-003 (`8b50343`) |
|---|---|---|
| E2E-13b SSE `grupoId` ajeno, PDP permite la ruta | **500**, sin cuerpo JSON | **403 `FORBIDDEN`**, `application/json`, `status=403` en el cuerpo, `X-Decision-Id` presente (PDP permitió), denegación en Application (log del handler con el mismo correlation ID), gauge `realtime_subscribers_active` sin cambios |
| E2E-13e sin `grupoId` | 400 (sin comprobar cuerpo) | 400 `VALIDATION_ERROR` JSON |
| E2E-13f `grupoId` no UUID | no existía | 400 `INVALID_REQUEST` JSON |

IDs (corrida sobre el principal): E2E-13b correlation `0028c191-2952-430e-886c-d83db1803803`, decisión
`5c15e034-86eb-4f3b-a351-61c4d9c9fc8c`; sobre la DB aislada: `6e515404-ea60-42da-a0d2-19db1f2b19c4` /
`ae95ea6e-d681-454d-8f80-4c652fed7c82`.

**Observación (E2E-13g):** el gauge leía 1 durante la matriz porque el stream de E2E-13a, cerrado por el cliente, sigue
contado hasta que una escritura de heartbeat falla; se observó el drenaje a 0 unas 2 min después. No es una suscripción
nueva (13b no la incrementa), pero sí retención de recursos: deuda SB3-12 en [TECHNICAL_DEBT](TECHNICAL_DEBT.md).

## Aprovisionamiento

`role_matrix_e2e.py provision` (efectos declarados en su docstring): client roles `ADMINISTRADOR`, `DECANO`,
`COORDINADOR` en `asistencias-api` y su scope mapping en `asistencias-lab-e2e`; 9 usuarios nuevos del realm del
laboratorio; rol PDP `MATRIZ_ROLES_E2E_LAB` con 27 recursos exactos estáticos + recursos dinámicos (ids de sesión,
reclamo y soporte creados durante la corrida). No modifica recursos, roles ni asignaciones previas. Keycloak 8081 no se toca.

| Usuario lab | `idUsuario` (prefijo) | Origen del `idUsuario` (verificado en SQL aislado) | Client role |
|---|---|---|---|
| `lab-e2e-administrador` | `e1f2a3b4-…-0006` | única fila de `uv_administrador` | ADMINISTRADOR |
| `lab-e2e-decano` | `e1f2a3b4-…-0001` | única fila de `uv_decano` | DECANO |
| `lab-e2e-coordinador` | `e1f2a3b4-…-0002` | `E2E_COORDINADOR_ID_USUARIO` (identidad E2E institucional existente) | COORDINADOR |
| `lab-e2e-docente-seed` | `e1f2a3b4-…-0003` | `E2E_DOCENTE_ID_USUARIO`; titular de `{G001}` | DOCENTE |
| `lab-e2e-estudiante` | `e1f2a3b4-…-0004` | `E2E_ESTUDIANTE_ID_USUARIO`; matriculado en `{G001}` | ESTUDIANTE |
| `lab-e2e-docente-fixpag1000` / `-5000` | `de000003-…-0002/0003` | fixture FIX-PAG (MAINT-003L) | DOCENTE |
| `lab-e2e-revocable` | `de000003-…-0001` | fixture FIX-PAG-300 (cuenta dedicada a revocación) | DOCENTE |
| `lab-e2e-sin-rol` | `e1f2a3b4-…-0003` | `E2E_DOCENTE_ID_USUARIO` | ninguno |

Claims verificados para los 9 (evidencia `role-matrix-provisioning.json`): RS256, `kid` presente en el JWKS, `iss`
exacto del laboratorio, `aud` ⊇ {`security-baseline-pdp`, `asistencias-api`}, roles = el esperado (ninguno para
`sin-rol`), `idUsuario` = el verificado, `sub` = id Keycloak ≠ `idUsuario`, vida 300 s. Todos PASS. El `aud` incluye
además `security-baseline-bff` (lo añade el scope upstream `security-baseline-claims`; ya ocurría en 002, deuda SB3-16).

Hallazgo de identidad: `E2E_ADMIN_ID_USUARIO` del realm institucional 8081 **no existe** en la DB (ver
`role-identity-verification.json`); por eso el ADMINISTRADOR del laboratorio usa la fila semilla de la DB.

## RBAC

| Ruta (GET) | ADMIN | DECANO | COORD | DOCENTE seed | ESTUDIANTE | sin rol | DOCENTE ajeno |
|---|---:|---:|---:|---:|---:|---:|---:|
| `/api/v1/admin/decanos` | 200 | 403 | 403 | 403 | 403 | 403 | 403 |
| `/api/v1/admin/sedes` | 501 | 403 | 403 | 403 | 403 | 403 | 403 |
| `/api/v1/decano/coordinadores` | 403 **KNOWN_DEFECT** | 200 | 403 | 403 | 403 | 403 | 403 |
| `/api/v1/coordinador/periodos-academicos` | 200 | 403 | 200 | 403 | 403 | 403 | 403 |
| `/api/v1/coordinador/docentes` | 501 | 403 | 501 | 403 | 403 | 403 | 403 |
| `/api/v1/docente/horarios` | 403 | 403 | 403 | 200 | 403 | 403 | 200 |
| `/api/v1/estudiante/horarios` | 403 | 403 | 403 | 403 | 200 | 403 | 403 |
| `/api/v1/docentes` | 200 | 403 | 200 | 403 | 403 | 403 | 403 |
| `/api/v1/estudiantes` | 200 | 403 | 200 | 403 | 403 | 403 | 403 |
| `/api/v1/grupos` | 200 | 403 | 200 | 403 | 403 | 403 | 403 |
| `/api/v1/usuarios/perfil` | 501 | 501 | 501 | 501 | 501 | 501 | 501 |
| `/api/v1/tipos-identificacion` | 200 | 200 | 200 | 200 | 200 | 200 | 200 |
| `/api/v1/realtime/status` | 200 | 200 | 200 | 200 | 200 | 200 | 200 |
| `/api/v1/grupos/{G001}/estudiantes` | 200 | 403 | 200 | 200 | 403 | 403 | 403 |

### PDP sin concesión

| Caso | Status | Code | Correlation ID | Decision ID | Capa | Resultado |
|---|---:|---|---|---|---|---|
| RB-pdp-nogrant ADMINISTRADOR en ruta no concedida por el PDP | 403 | FORBIDDEN | `98b7adfd-9611-421b-b34d-6a94348e6b7a` | `—` | PEP/PDP deny | PASS |

### Volumen y paginación

| Caso | Status | Code | Correlation ID | Decision ID | Capa | Resultado |
|---|---:|---|---|---|---|---|
| VOL-300 estudiantes del grupo propio FIX-PAG-300 | 200 | — | `4335d544-5d39-4b88-8168-6fff215ff24e` | `65bfb77f-8db6-49b4-be79-b03b20533a36` | PEP/PDP allow + Spring + Application | PASS — 0.43s |
| VOL-1000 estudiantes del grupo propio FIX-PAG-1000 | 200 | — | `b429e6bc-7bc3-40c7-99af-32e83f7ef739` | `8ec54417-b0c3-48a7-abc2-2fbbfb871e29` | PEP/PDP allow + Spring + Application | PASS — 0.43s |
| VOL-5000 estudiantes del grupo propio FIX-PAG-5000 | 200 | — | `a0b43e34-58b6-47c9-b440-c3e7e7678011` | `4f88f5b9-1553-4699-abcf-4a77d4e7c557` | PEP/PDP allow + Spring + Application | PASS — 0.52s |
| VOL-DIR-p0 directorio FIX-PAG-5000 página 0 | 200 | — | `45f45fb8-9d94-4f9b-98f8-d1cfd059598e` | `e1dbf23d-333f-4d30-991e-9ef908be0436` | PEP/PDP allow + Spring + Application | PASS |
| VOL-DIR-p49 directorio FIX-PAG-5000 página 49 | 200 | — | `4a78e43a-c445-43ad-8f57-33de2aadf25d` | `a7d13dac-6938-4248-be21-b3d233938a57` | PEP/PDP allow + Spring + Application | PASS |
| VOL-DIR-p50 directorio FIX-PAG-5000 página 50 | 200 | — | `f1475124-ca02-4299-8a1d-5d7a9b074267` | `01f9729a-d70e-4b46-8d9d-ef532efb008f` | PEP/PDP allow + Spring + Application | PASS |
| VOL-DIR-docente directorio general (no se abre a DOCENTE) | 403 | FORBIDDEN | `92b0b80f-df40-421f-96b4-1a4337f1e5c1` | `47dd6043-c227-4da0-9099-71d3923d8e61` | Spring AuthorizationFilter (rol local) | PASS |

### Sesiones UTC v2/v1 (escrituras en DB aislada)

| Caso | Status | Code | Correlation ID | Decision ID | Capa | Resultado |
|---|---:|---|---|---|---|---|
| WR-01 POST /api/v2/sesiones (offset -05:00) | 201 | — | `3c35c6bd-6b0f-4112-a257-77e2d57fcd34` | `502726c0-12cb-4ab1-94fe-2924f4f15e7d` | PEP/PDP allow + Spring + Application | PASS |
| WR-02 GET v2 por grupo: sesión creada en UTC | 200 | — | `717920e3-8077-4222-957b-45f2535fca63` | `364eb886-f2ec-4f26-bfc2-f66f0fd703a6` | PEP/PDP allow + Spring + Application | PASS |
| WR-03 PATCH v2 (offset +02:00) | 200 | — | `37347bf0-0851-45b8-b6f0-91ca5f1b10f4` | `db5b261f-6c06-4f00-b609-2e47a160fd5b` | PEP/PDP allow + Spring + Application | PASS |
| WR-04 GET v2: instantes convertidos a UTC | 200 | — | `a3036b14-4a6c-4984-9012-16a407a141d5` | `b2c5857d-ed75-452e-9b37-21043fe284e9` | PEP/PDP allow + Spring + Application | PASS |
| WR-05 GET v1: mismo instante UTC sin offset (LocalSessionDateTime) | 200 | — | `bec65b61-66f7-45b5-93d6-59a1e02227c8` | `e0c5a223-e162-4c05-90d0-cf9c5b5d3c34` | PEP/PDP allow + Spring + Application | PASS — v1 inicio=2026-11-03T13:00:00 fin=2026-11-03T16:30:00 |
| WR-06 PUT v1 legacy (hora local) | 200 | — | `823d2fa5-537f-4f13-8b78-aed882cbbadd` | `3f5d5254-b257-485b-8a08-aa8b868eb3c9` | PEP/PDP allow + Spring + Application | PASS |
| WR-07 GET v2 tras escritura v1: INDETERMINADA (contrato UTC-D07) | 200 | — | `1db40911-82b5-467a-a537-808e56130228` | `28faf375-73aa-4eba-b7f8-ae444c69eb99` | PEP/PDP allow + Spring + Application | PASS |
| WR-08 PATCH v1 | 200 | — | `77ae2ee7-b3d6-47a1-be80-f2083410099e` | `a8ecccdd-13ee-4c81-952b-b93ca2c6be97` | PEP/PDP allow + Spring + Application | PASS |
| WR-09 PATCH v2 restablece CONFIRMADA | 200 | — | `6b03396c-41ad-4d3a-b16f-78e1b0a9c23f` | `e3fc796e-7ff9-49d5-ab07-511fa5e8efeb` | PEP/PDP allow + Spring + Application | PASS |
| WR-10 PUT v2 inexistente (UTC-D09) | 403 | FORBIDDEN | `63576a6c-da0a-4c10-9ad6-2a998832a82b` | `—` | PEP/PDP deny o Spring 405 | PASS — 403 = el PDP no concede PUT v2 (deny-by-default); 405 = Spring |
| WR-11 POST v2 en grupo ajeno (FIX-PAG-300) | 403 | FORBIDDEN | `7b9ba633-c032-4d16-95d9-4ae10c45acb4` | `be10a5f6-b619-4e16-877e-266d1e27e0e4` | Application / InstitutionalScopePort (titularidad) | PASS |
| WR-12 PATCH v2 de sesión ajena (docente FIX-PAG-1000) | 403 | FORBIDDEN | `cfc46354-600f-4a67-91ad-965ca5890255` | `d7bdbe09-e6f3-417c-8268-ae4541497502` | Application / InstitutionalScopePort (titularidad) | PASS |
| WR-13 POST v2 con rol ESTUDIANTE | 403 | FORBIDDEN | `0240b71c-5c0a-4a86-93e5-97c6c1f74399` | `e002a603-e2f3-4218-910b-f68a40bdfccb` | Spring AuthorizationFilter (rol local) | PASS |
| WR-14 POST v2 sin token | 401 | UNAUTHORIZED | `c2d9bdab-5ec4-448e-9e3a-071f7fdc9c9f` | `—` | Spring Resource Server / sin credencial | PASS |
| WR-15 sin escritura tras los 403/401 | 200 | — | `—` | `—` | SQL aislado (lectura) | PASS |

### Asistencias

| Caso | Status | Code | Correlation ID | Decision ID | Capa | Resultado |
|---|---:|---|---|---|---|---|
| AS-01 POST asistencias/lote por el docente titular | 201 | — | `c7b2c582-d5f2-42f0-85d7-1f576f75d90b` | `86d280e6-8499-4adc-8136-e6b7b1c43782` | PEP/PDP allow + Spring + Application | PASS |
| AS-02 POST asistencias/lote por docente ajeno | 403 | ERR_DOCENTE_SIN_TITULARIDAD_SESION | `ffe5eb92-0460-431c-8d60-40850b6fbd52` | `0a734e61-5ec2-4fc2-8c22-6563e577bac9` | Application / InstitutionalScopePort (titularidad) | PASS |
| AS-03 GET asistencias del grupo/sesión | 200 | — | `e71c7f25-8410-4d83-b3fb-c61cb23177dd` | `b24cec23-ddb0-43e5-be09-65629f3b4911` | PEP/PDP allow + Spring + Application | PASS |

### Reclamos (solicitudes de revisión)

| Caso | Status | Code | Correlation ID | Decision ID | Capa | Resultado |
|---|---:|---|---|---|---|---|
| RC-01 POST revisión por docente (rol no permitido) | 403 | FORBIDDEN | `c5c23fd8-bb6e-41cf-a748-3193eca2438f` | `bd057596-a659-42a1-94cd-ddeae4912926` | Spring AuthorizationFilter (rol local) | PASS |
| RC-02 POST revisión por el estudiante | 202 | — | `24b2801b-b40b-4ff2-aa17-8a29aaaab90f` | `7ead9160-b066-433f-af3f-a1f1c4ee271b` | PEP/PDP allow + Spring + Application | PASS |
| RC-03 GET /api/v1/docente/reclamos (listado del docente) | 501 | FEATURE_UNAVAILABLE | `9e2749af-45d6-4450-b0ef-d31106485a69` | `c573d3ac-82a9-4c44-aada-db0ddd6f21c1` | Controller (FEATURE_UNAVAILABLE) | PASS — 501 = FEATURE_UNAVAILABLE: el docente no puede listar reclamos por API; id localizado en la DB aislada |
| RC-04 PATCH reclamo por docente ajeno | 403 | FORBIDDEN | `c946c339-6e45-49f9-a064-82a81553602c` | `f857d822-cb44-4bda-a3e8-ede80cc8fa0c` | Application / InstitutionalScopePort (titularidad) | PASS |
| RC-05 PATCH reclamo por el docente titular | 200 | — | `430d316b-bf7b-4631-8e82-519ca15dce60` | `688418d9-45ec-4b36-bb3c-3343a788ac59` | PEP/PDP allow + Spring + Application | PASS |
| RC-06 estado persistido tras aprobar | 200 | — | `—` | `—` | SQL aislado (lectura) | PASS — estado=aceptada |

### DELETE

| Caso | Status | Code | Correlation ID | Decision ID | Capa | Resultado |
|---|---:|---|---|---|---|---|
| DEL-01 DELETE reclamo por el estudiante | 501 | FEATURE_UNAVAILABLE | `df972d32-4f2f-43d2-b70a-c1090cab5569` | `e4af9b78-16f1-49e0-bb3b-61068a99e91d` | Controller (FEATURE_UNAVAILABLE) | PASS |
| DEL-02 DELETE reclamo por docente (rol) | 403 | FORBIDDEN | `cd4494b3-4008-449b-87f3-f8621ccd6958` | `44d16395-9e1e-42b6-a467-10f0c669a2b5` | Spring AuthorizationFilter (rol local) | PASS |
| DEL-03 DELETE matrícula por COORDINADOR | 501 | FEATURE_UNAVAILABLE | `5d511a57-3449-43c6-ae9b-9f470e731da8` | `6f981a61-bfff-435c-8dd0-4d206b899883` | Controller (FEATURE_UNAVAILABLE) | PASS |
| DEL-04 DELETE matrícula por DOCENTE (rol) | 403 | FORBIDDEN | `4166ae11-88e5-4a0e-abf1-7f9a8bd710c0` | `1965bbb1-6bf4-408f-9c27-332970df345e` | Spring AuthorizationFilter (rol local) | PASS |
| DEL-05 matrícula intacta tras los DELETE | 200 | — | `—` | `—` | SQL aislado (lectura) | PASS |

### Descargas

| Caso | Status | Code | Correlation ID | Decision ID | Capa | Resultado |
|---|---:|---|---|---|---|---|
| DL-01 reporte Excel por el docente titular | 200 | — | `62c69842-f1ef-424e-987c-156c704777f4` | `0c1713c7-a10b-4444-89dd-666c7c71e190` | PEP/PDP allow + Spring + Application | PASS — bytes=4343 |
| DL-02 reporte Excel por COORDINADOR | 200 | — | `119f6982-b998-4551-b759-3482f16b32b9` | `253467fe-cc04-4d88-824d-0443a6c9f696` | PEP/PDP allow + Spring + Application | PASS — bytes=4343 |
| DL-03 reporte Excel por docente AJENO (titularidad) | 200 | — | `5da1f82b-7f16-4af0-8535-6cfc864d5d51` | `b85a8a11-8815-4526-bfbb-bf41b75c54f1` | Application / InstitutionalScopePort (titularidad) | **KNOWN_DEFECT** — bytes=4344; status 200 not in ['403']; SECURITY_FINDING: el reporte Excel de asistencia no valida titularidad del docente (IDOR) |
| DL-04 reporte Excel por ESTUDIANTE (rol) | 403 | FORBIDDEN | `fe428e8b-acc2-424c-ac0e-611c78d38d42` | `5d73b7fd-f50d-40e8-8ee1-de19f08d9fb0` | Spring AuthorizationFilter (rol local) | PASS — bytes=305 |
| DL-05 subir soporte (ESTUDIANTE, ClamAV + MinIO) | 201 | — | `377895f7-a146-41e4-aa3a-28c57f6f182f` | `dd194f0f-8fd9-4146-87d2-cf4eb28b7b59` | PEP/PDP allow + Spring + Application | PASS — fileId=sí |
| DL-06 subir soporte con rol DOCENTE | 403 | FORBIDDEN | `1651546b-3b0d-4cd4-8f0f-faa5f5b76675` | `952f760b-a5d6-4f04-a0b9-6a00c120cf73` | Spring AuthorizationFilter (rol local) | PASS |
| DL-07 descargar soporte propio | 200 | — | `77608429-fb1a-45e3-98da-40caf45d11a4` | `0649c0bf-2301-4ac3-a7d2-f6ebcfccfe5c` | PEP/PDP allow + Spring + Application | PASS |
| DL-08 descargar soporte ajeno (DOCENTE titular) | 404 | RESOURCE_NOT_FOUND | `a0e6345c-8e38-4163-96e2-51e23432cdd4` | `0ded9dbb-6b07-4348-9d90-190962a44e97` | Application / InstitutionalScopePort (titularidad) | PASS — contrato LB-004: 404 para no propietario, cualquier rol |

### CORS

| Caso | Status | Code | Correlation ID | Decision ID | Capa | Resultado |
|---|---:|---|---|---|---|---|
| CO-01 preflight desde origen permitido | 200 | — | `36ae4890-67b1-4844-ab79-77b62d78be51` | `—` | Spring CorsFilter | PASS |
| CO-02 preflight desde origen no permitido | 403 | — | `5206202e-2243-46be-a36b-d8319bc32b93` | `—` | Spring CorsFilter | PASS |

### CSRF / CORS en escrituras

| Caso | Status | Code | Correlation ID | Decision ID | Capa | Resultado |
|---|---:|---|---|---|---|---|
| CS-01 POST con cookies y sin Bearer (CSRF, sin Origin) | 401 | UNAUTHORIZED | `4baa9503-edda-4105-b3aa-c055954dbfc2` | `—` | Spring Resource Server / sin credencial | PASS |
| CS-01b POST con cookies desde origen no permitido | 403 | — | `b349dab7-8884-4a8d-8bd1-dbc4165c3589` | `—` | Spring CorsFilter | PASS |
| CS-02 POST con Bearer desde origen no permitido | 403 | — | `fd1876d3-7062-4c54-9aab-28efff5a6cf1` | `—` | Spring CorsFilter | PASS |
| CS-03 respuesta autenticada sin sesión ni cookies | 200 | — | `e9b722a0-39b5-4444-b16d-32d120e2be7b` | `b793ccfd-bbbd-4917-ad8f-fdcccfc505ca` | PEP/PDP allow + Spring + Application | PASS |
| CS-04 ninguna sesión creada por CS-01/CS-02 | 200 | — | `—` | `—` | SQL aislado (lectura) | PASS |

### SSE por rol

| Caso | Status | Code | Correlation ID | Decision ID | Capa | Resultado |
|---|---:|---|---|---|---|---|
| SSE-docente stream del grupo semilla | 200 | — | `95555ba4-b208-4db2-af94-c0e0c47c3b2f` | `d7e1db19-98f7-4bf4-a327-4a48a140105f` | PEP/PDP allow + Spring + Application | PASS |
| SSE-estudiante stream del grupo semilla | 403 | FORBIDDEN | `1e3e5160-7628-4046-b849-5dcfa3d7d317` | `a4297c7e-40d4-4471-b014-178aebee63e8` | Application / InstitutionalScopePort (titularidad) | PASS |
| SSE-coord stream del grupo semilla | 403 | FORBIDDEN | `4b1eb127-9902-4eb0-8db4-79decbd9263d` | `0f0250c9-ad53-4a08-9655-cad84810f153` | Application / InstitutionalScopePort (titularidad) | PASS |

### Revocación y usuario deshabilitado

| Caso | Status | Code | Correlation ID | Decision ID | Capa | Resultado |
|---|---:|---|---|---|---|---|
| REV-01 token vigente antes de revocar | 200 | — | `3651ca79-bff6-4358-abc0-a83b35343bf7` | `8f31d380-272b-45a7-9c9e-b5c63e704b88` | PEP/PDP allow + Spring + Application | PASS |
| REV-02 mismo token tras cerrar todas las sesiones del usuario | 200 | — | `000b7b84-8329-4b75-b491-d16f689ac092` | `1ff67355-3891-495d-a9a1-ae71ec600ef0` | JwtDecoder local + PEP/PDP | **KNOWN_DESIGN_LIMIT** — X-Decision-Id=sí; status 200 not in ['401', '403']; JWT validado localmente sin introspección: la revocación de sesiones no invalida el access token hasta exp |
| REV-03 nuevo login de usuario deshabilitado | 400 | — | `—` | `—` | Keycloak | PASS — error=invalid_grant |
| REV-04 token previo de usuario deshabilitado | 200 | — | `f45bb1b0-4283-4d3d-a7bb-379ab6755200` | `a83ffce6-4565-452c-94f2-c1b6fe0744de` | JwtDecoder local + PEP/PDP | **KNOWN_DESIGN_LIMIT** — X-Decision-Id=sí; status 200 not in ['401', '403']; JWT validado localmente sin introspección: deshabilitar al usuario no invalida el access token vigente |
| REV-05 usuario rehabilitado (estado del laboratorio restablecido) | 200 | — | `832ad3d4-150d-422d-b0b0-31a386efc864` | `ed778815-1200-43b5-b77a-571313eabad1` | PEP/PDP allow + Spring + Application | PASS |

### Backend con login SQL de solo lectura

| Caso | Status | Code | Correlation ID | Decision ID | Capa | Resultado |
|---|---:|---|---|---|---|---|
| RO-HTTP-01 lectura con login de solo lectura | 200 | — | `e82f9d80-5237-481f-9396-3cf4673c989c` | `f773253d-368b-4d9b-8c52-a1b0201fab71` | PEP/PDP allow + Spring + Application | PASS |
| RO-HTTP-02 escritura con login de solo lectura: SQL Server la deniega | 500 | DATABASE_OPERATION_ERROR | `b744e51b-0536-4ab1-bb80-e28d8bbc2730` | `dbb076c8-ebd1-4b21-a520-20ddb46f6be6` | SQL Server (permiso EXECUTE denegado) | PASS |

### PDP detenido

| Caso | Status | Code | Correlation ID | Decision ID | Capa | Resultado |
|---|---:|---|---|---|---|---|
| E2E-16-pre PEP con PDP detenido (directo) | 503 | PDP_CREDENTIAL_VALIDATION_UNAVAILABLE | `sec-lab-84fd53d6-1ca3-457a-86fa-86f0e256cfe0` | `—` | PEP directo (precondición) | PASS |
| E2E-16a PDP inaccesible: horarios | 503 | ACCESS_DECISION_UNAVAILABLE | `aefc9898-cc6e-43f4-b739-99705bd16506` | `—` | PEP client fail-closed | PASS |
| E2E-16b PDP inaccesible: SSE | 503 | ACCESS_DECISION_UNAVAILABLE | `bad593dc-59ad-471f-a810-011a3503def8` | `—` | PEP client fail-closed | PASS |
| E2E-16c PDP inaccesible: sin token | 401 | UNAUTHORIZED | `d8bef579-7fbf-42f8-acb8-26d5df4657cb` | `—` | Spring Resource Server / sin credencial | PASS |
| E2E-16d PDP inaccesible: actuator | 200 | — | `34f46ff7-49ab-4dd8-87c0-172eaebde7bf` | `—` | Ruta pública (sin PEP) | PASS |

### Backend estable (mismo jar, flag PEP OFF, issuer institucional 8081)

| Caso | Status | Code | Correlation ID | Decision ID | Capa | Resultado |
|---|---:|---|---|---|---|---|
| ST-01 horarios docente institucional | 200 | — | `21c90150-c139-413f-be35-32a3ef65bc1e` | `—` | Backend estable (PEP OFF) | PASS |
| ST-02 estudiantes del grupo propio | 200 | — | `6e0c4e2e-ce22-454c-833f-db2902df82ec` | `—` | Backend estable (PEP OFF) | PASS |
| ST-03 estudiantes del grupo ajeno | 403 | FORBIDDEN | `28e58871-2beb-4d6f-a65e-1afb4dbf3338` | `—` | Backend estable (PEP OFF) | PASS |
| ST-04 SSE grupo propio | 200 | — | `c2212b6e-6910-4ca6-92c9-1612fd92d91f` | `—` | Backend estable (PEP OFF) | PASS |
| ST-05 SSE grupo ajeno | 403 | FORBIDDEN | `66121f9c-5516-45be-8d74-45c7d66d7b9f` | `—` | Backend estable (PEP OFF) | PASS |
| ST-06 SSE sin grupoId | 400 | VALIDATION_ERROR | `9c881a99-1781-48ff-ae67-6a52e88b0106` | `—` | Backend estable (PEP OFF) | PASS |
| ST-07 token del laboratorio (issuer 19090) rechazado | 401 | UNAUTHORIZED | `166b9971-730b-47a6-b1e9-db0e95510d37` | `—` | Backend estable (PEP OFF) | PASS |
| ST-08 sin token | 401 | UNAUTHORIZED | `098c4fe8-a922-41c1-93b3-882d549af0ac` | `—` | Backend estable (PEP OFF) | PASS |
| ST-09 actuator público | 200 | — | `02ced3a9-c355-4982-b88f-03940f46c5be` | `—` | Ruta pública (sin PEP) | PASS |

### E2E SEC-BASELINE-002 repetido tras FIX-001 (DB principal, solo lectura)

| Caso | Status | Code | Correlation ID | Decision ID | Capa | Resultado |
|---|---:|---|---|---|---|---|
| E2E-01a horarios del docente FIX-PAG-300 | 200 | — | `a59346a5-5910-4cb3-b1da-db192b25ebf2` | `f568ebe4-f295-4dbe-ad43-c503de291f43` | PEP/PDP allow + Spring + Application | PASS |
| E2E-01b estudiantes del grupo propio FIX-PAG-300 | 200 | — | `94be304b-4c05-46c9-898e-ad6f81faa601` | `2f23f1eb-73e2-4907-a61a-efa6c446bbad` | PEP/PDP allow + Spring + Application | PASS |
| E2E-02-pre PDP concede sesiones v2 del grupo ajeno | 204 | — | `sec-lab-07839bb1-6842-4bde-a5a5-e18c1c11503a` | `cec56437-a5f1-4f0b-a6ad-d89657dd0e84` | PEP/PDP directo (precondición) | PASS — decisión del PDP para la misma identidad y ruta, sin backend |
| E2E-02a sesiones v2 del grupo ajeno FIX-PAG-1000 | 403 | FORBIDDEN | `f6168b31-974a-43e3-b3ec-94ada36ce544` | `acbfa051-201c-497b-b5f3-0c01949fb522` | Application / InstitutionalScopePort (titularidad) | PASS |
| E2E-02-pre PDP concede estudiantes del grupo ajeno | 204 | — | `sec-lab-a2a82555-ba68-48b8-9b74-20dade54abd9` | `960bd066-0180-45ed-ab0b-ae884e925635` | PEP/PDP directo (precondición) | PASS — decisión del PDP para la misma identidad y ruta, sin backend |
| E2E-02b estudiantes del grupo ajeno FIX-PAG-1000 | 403 | FORBIDDEN | `a6ea7010-5de7-44f0-84c4-8202af1509d5` | `32c3c272-aeb3-43e1-acea-154e5116cfba` | Application / InstitutionalScopePort (titularidad) | PASS |
| E2E-03-pre PDP deniega a la identidad sin asignación | 403 | ACCESS_DENIED | `sec-lab-45b058d8-314b-4ee2-9912-7b5a76c044a1` | `5ecc8a6c-0993-4171-a317-9b73796b38e8` | PEP/PDP directo (precondición) | PASS — decisión del PDP para la misma identidad y ruta, sin backend |
| E2E-03 docente sin asignación PDP | 403 | FORBIDDEN | `f8f6c288-7e69-463d-a3dd-d974d6ea0786` | `—` | PEP/PDP deny | PASS |
| E2E-04a rol ESTUDIANTE en horarios de docente | 403 | FORBIDDEN | `5ac2800f-9576-4488-aa99-464e1025d59c` | `15916f3e-a39c-495c-8bf7-8432865232cf` | Spring AuthorizationFilter (rol local) | PASS |
| E2E-04b rol ESTUDIANTE, titular de FIX-PAG-1000, sesiones v2 | 403 | FORBIDDEN | `4333e1e0-704f-46bd-89dc-79b9d6a659f4` | `18a7a2d2-9661-408f-b79f-97b6cbd53dea` | Spring AuthorizationFilter (rol local) | PASS |
| E2E-05 JWT sin idUsuario | 401 | UNAUTHORIZED | `896f4c51-22bb-4f87-8148-cc73ab78c886` | `—` | Spring Resource Server / JwtDecoder | PASS — única diferencia con E2E-01: falta el claim idUsuario |
| E2E-06 audiencia sin asistencias-api | 401 | UNAUTHORIZED | `d40d8a01-f034-4fea-9f2f-eafaf6673d26` | `—` | Spring Resource Server / JwtDecoder | PASS — única diferencia: aud sin asistencias-api |
| E2E-07 issuer institucional :8081 (no autorizado en 8083) | 401 | UNAUTHORIZED | `418c459b-50dc-4a2f-85fd-c4b807820135` | `—` | Spring Resource Server / JwtDecoder | PASS — token válido de otro issuer |
| E2E-08 firma alterada | 401 | UNAUTHORIZED | `fcdaa998-ef8e-46e0-afa9-a49fba099669` | `—` | Spring Resource Server / JwtDecoder | PASS — último carácter de la firma modificado |
| E2E-09 sin token | 401 | UNAUTHORIZED | `881b96cb-4e6b-46f1-a3fc-3d842dce7e8f` | `—` | Spring Resource Server / sin credencial | PASS |
| E2E-12 sesiones UTC v2 del grupo propio | 200 | — | `851fbf56-8e55-4d2f-9070-9d939f48192f` | `baf53d93-67ab-4212-ae76-70d6f7642b24` | PEP/PDP allow + Spring + Application | PASS — UTC_V2=4 |
| E2E-13a SSE autorizado (grupo propio) | 200 | — | `53a0a0fc-6dc5-4247-acbb-675ac5a4e261` | `3fd73d93-4f5c-4f38-9b3e-9dac900d8d5d` | PEP/PDP allow + Spring + Application | PASS |
| E2E-13b SSE grupo ajeno (PDP permite la ruta) | 403 | FORBIDDEN | `0028c191-2952-430e-886c-d83db1803803` | `5c15e034-86eb-4f3b-a351-61c4d9c9fc8c` | Application / InstitutionalScopePort (titularidad) | PASS |
| E2E-13c SSE sin asignación PDP | 403 | FORBIDDEN | `7cf45914-9d64-4b1c-9465-18697ce91c2d` | `—` | PEP/PDP deny | PASS |
| E2E-13d SSE sin token | 401 | UNAUTHORIZED | `19dd2b55-a8a2-4e7c-bc2c-b78567a5c68e` | `—` | Spring Resource Server / sin credencial | PASS |
| E2E-13e SSE sin grupoId (contrato: obligatorio) | 400 | VALIDATION_ERROR | `1fbea8fe-d1bf-4d11-9f22-51d9d19c43c7` | `53117fbf-7750-4a28-9fa3-2f2cab132572` | Controller (validación de contrato) | PASS |
| E2E-13f SSE grupoId no UUID | 400 | INVALID_REQUEST | `26ac5f8f-2ee8-4252-a737-03c09a2bdc00` | `8e4a8237-7b82-4b8d-a9a7-32a31a89abca` | Controller (validación de contrato) | PASS |
| E2E-13g sin suscripciones residuales tras la matriz SSE | 200 | — | `—` | `—` | Observabilidad | PASS — realtime_subscribers_active antes=1.0 después=1.0 |
| E2E-14 UUID del grupo propio en mayúsculas (ruta no registrada) | 403 | FORBIDDEN | `82f38b56-f0eb-405d-95ec-f0727989d7ce` | `—` | PEP/PDP deny | PASS |
| E2E-15 actuator público | 200 | — | `79311cbb-a6a3-4c54-ae11-cafb1c25d6eb` | `—` | Ruta pública (sin PEP) | PASS |

### PEP detenido y respuestas anómalas del PEP (stub)

| Caso | Status | Code | Correlation ID | Decision ID | Capa | Resultado |
|---|---:|---|---|---|---|---|
| E2E-10a PEP inaccesible: horarios | 503 | ACCESS_DECISION_UNAVAILABLE | `3ed9999d-f7f1-4608-ba34-a0cb64a57995` | `—` | PEP client fail-closed | PASS |
| E2E-10b PEP inaccesible: SSE | 503 | ACCESS_DECISION_UNAVAILABLE | `fed567a4-32ea-4a3b-8316-257ac663cde1` | `—` | PEP client fail-closed | PASS |
| E2E-10c PEP inaccesible: sin token | 401 | UNAUTHORIZED | `c1598f0f-50a9-41ea-9154-4f2435278311` | `—` | Spring Resource Server / sin credencial | PASS |
| E2E-10d PEP inaccesible: actuator | 200 | — | `8364f807-318f-4069-a438-aa622f749fc5` | `—` | Ruta pública (sin PEP) | PASS |
| E2E-11a PEP 200 vacío sin decisión (comportamiento PEP-03) | 503 | ACCESS_DECISION_UNAVAILABLE | `b90ec594-540c-4ded-9fa1-6fcef72dce74` | `—` | PEP client fail-closed | PASS |
| E2E-11b PEP 204 sin X-Decision-Id | 503 | ACCESS_DECISION_UNAVAILABLE | `5fa70eba-b584-4c7a-a4e2-9d8668367df2` | `—` | PEP client fail-closed | PASS |
| E2E-11c PEP 204 con X-Decision-Id inválido | 503 | ACCESS_DECISION_UNAVAILABLE | `5cf26a96-c98b-4561-a286-e2721b779a1d` | `—` | PEP client fail-closed | PASS |
| E2E-11d PEP 500 | 503 | ACCESS_DECISION_UNAVAILABLE | `7cc50f93-4a55-4b8e-8cf1-c51f6903191a` | `—` | PEP client fail-closed | PASS |
| E2E-11e PEP 302 redirect | 503 | ACCESS_DECISION_UNAVAILABLE | `41b40f1f-1553-4dd0-b629-52e5e2495c00` | `—` | PEP client fail-closed | PASS |
| E2E-11f PEP 200 con X-Decision-Id válido (2xx distinto de 204) | 503 | ACCESS_DECISION_UNAVAILABLE | `a6ef3ee2-a8cd-4ec0-8904-c7653b91d5e0` | `—` | PEP client fail-closed | PASS |
| E2E-11g PEP excede el timeout | 503 | ACCESS_DECISION_UNAVAILABLE | `c13d958f-d134-4e51-847b-2f66bf6788df` | `—` | PEP client fail-closed | PASS |

### PEP/PDP directo (sin backend)

| Caso | Status | Code | Correlation ID | Decision ID | Capa | Resultado |
|---|---:|---|---|---|---|---|
| PEP-01 sin credencial de aplicación | 401 | INTEGRATION_TOKEN_INVALID | `sec-lab-a09c5798-e0bc-43ec-b58a-d079ac5752f6` | `—` | — | PASS |
| PEP-02 credencial de aplicación incorrecta | 401 | INTEGRATION_TOKEN_INVALID | `sec-lab-5c933d64-3fa5-4ad5-8958-7574ed3c9b68` | `—` | — | PASS |
| PEP-03 sin Bearer | 200 | — | `sec-lab-caa7e9d3-271e-4ba3-810d-c0000bd00cf0` | `—` | — | **KNOWN_UPSTREAM_FAIL** — PEP v0.1.6 responde 200 sin X-Decision-Id cuando falta el Bearer (fail-open); mitigado en el cliente; status 200 not in ['401'] |
| PEP-04 Bearer con firma alterada | 401 | TOKEN_INVALID | `sec-lab-819ca6d6-db34-44f1-8801-089578756369` | `—` | — | PASS |
| PEP-05 Escenario A: JWT institucional (issuer :8081) | 401 | TOKEN_INVALID | `sec-lab-2946eea9-8277-47d1-9358-d740bd46be98` | `—` | — | PASS |
| PEP-06 lab token con rol PDP sobre recurso concedido | 204 | — | `sec-lab-056d02cc-6049-49e0-90b8-87fccad1d701` | `2db7512d-35d5-44bf-9151-5b376bb23d2a` | — | PASS |
| PEP-07 lab token sin asignación PDP | 403 | ACCESS_DENIED | `sec-lab-2ef8ef36-1e8a-4d39-af67-0360a3ecfd1f` | `18bd0dbf-fdfe-4979-aa0e-ef0755f1ada8` | — | PASS |
| PEP-08 recurso registrado no concedido al rol | 403 | ACCESS_DENIED | `sec-lab-fca2ad1a-043e-41be-92f2-f34a43225b6b` | `2d026fb1-0957-4e42-8042-6c04a0ada3d9` | — | PASS |
| PEP-09 ruta no registrada | 403 | ACCESS_DENIED | `sec-lab-597b0599-28eb-4dc7-944c-1ec255fe723f` | `180e8974-0e87-4834-814f-7bad6e760e30` | — | PASS |
| PEP-10 método distinto al concedido | 403 | ACCESS_DENIED | `sec-lab-0374e8f4-8046-40fc-bfa5-2373de7c7090` | `40dae8ff-07fb-44b6-9299-67c2caf73881` | — | PASS |
| PEP-11 SSE concedido | 204 | — | `sec-lab-29abe4c7-24f8-4797-8e95-178a3becfb4d` | `a1cad07b-14df-4be4-8088-f3202342c528` | — | PASS |
| PEP-12 SSE sin asignación | 403 | ACCESS_DENIED | `sec-lab-56e75f84-b93b-4a52-844d-57de9035cecb` | `2f581c56-541c-4dea-98fb-47ae4fb03795` | — | PASS |
| PEP-13 applicationName ajeno con credencial propia | 401 | INTEGRATION_TOKEN_INVALID | `sec-lab-0f77eea6-09d8-4ae2-8a67-06d97abdf083` | `—` | — | PASS |
| PEP-14 environment distinto (prod) | 204 | — | `sec-lab-b5c87919-ce09-4c1b-9b07-2275c299e4cb` | `ca4e18eb-79be-4d11-983d-5a21c7def7a8` | — | PASS — registro informativo: el contrato v0.1.6 no ata el environment a la aplicación |

