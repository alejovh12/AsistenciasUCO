# HANDOFF — SEC-BASELINE-002

## Dictamen

**`PARTIAL_WITH_BLOCKERS`** — el recorrido completo JWT → Spring Security → PEP → PDP → RBAC → `InstitutionalScopePort`
→ SQL Server → HTTP funciona en local con una identidad de laboratorio reconocida por ambas capas; 30 de 31 peticiones
E2E a través del backend PASS (más 3 precondiciones directas al PEP, PASS). No es `FULL_LOCAL_E2E_PASS` porque:

1. E2E-13b (SSE con `grupoId` ajeno) responde **500** en lugar del **403** contractual (defecto preexistente del
   manejo de errores con `Accept: text/event-stream`; el acceso sí se deniega).
2. El aislamiento SQL es solo operacional (no hay DB E2E ni credencial de solo lectura; el backend usa `sa`).

Y bloqueos que impiden cualquier lectura institucional: la identidad es de laboratorio, la asociación
`idUsuario` es manual y el PDP v0.1.6 no autoriza rutas dinámicas.

## Respuestas explícitas

1. **GET académico real con 200: sí.** `GET /api/v1/docente/horarios` (1 registro = SQL),
   `GET /api/v1/grupos/{FIX-PAG-300}/estudiantes` (300 = SQL) y `GET /api/v2/sesiones/grupo/{FIX-PAG-300}`
   (6 = SQL, 4 `UTC_V2` con instantes idénticos) con `X-Decision-Id` real del PDP.
2. **Ownership con 403: sí.** El PDP concedió expresamente (204 + decisión) las rutas de FIX-PAG-1000 y el backend
   las denegó en Application (`InstitutionalScopePort`): E2E-02a (v2 sesiones) y E2E-02b (estudiantes). En SSE la
   titularidad también deniega, pero con status 500 (E2E-13b).
3. **Configuración que permitió una identidad reconocida por ambas capas:** realm `security-baseline` del Keycloak
   19090 con atributo de User Profile `idUsuario` (UUID, solo admin), cliente de recurso `asistencias-api` (roles
   `DOCENTE`, `ESTUDIANTE`) y cliente emisor `asistencias-lab-e2e` con el scope existente
   `security-baseline-claims` (aud `security-baseline-pdp`) + mappers oficiales `oidc-audience-mapper`
   (`asistencias-api`), `oidc-usermodel-client-role-mapper` (`resource_access.${client_id}.roles`) y
   `oidc-usermodel-attribute-mapper` (`idUsuario`); y el proceso 8083 con
   `KEYCLOAK_ISSUER_URI=http://keycloak.localhost:19090/realms/security-baseline`,
   `KEYCLOAK_EXPECTED_AUDIENCE=asistencias-api`, `KEYCLOAK_API_CLIENT_ID=asistencias-api`,
   `KEYCLOAK_USER_ID_CLAIM=idUsuario` (`start-backend-pep-lab.ps1 -IdentityMode SharedLab`). Sin cambios Java.
   Detalle: [KEYCLOAK_LAB_CONFIGURATION](KEYCLOAK_LAB_CONFIGURATION.md).
4. **Solo laboratorio:** el realm 19090 y todo lo creado en él; la asociación manual usuario → `idUsuario`; ROPC
   en `asistencias-lab-e2e`; issuer HTTP `keycloak.localhost`; `-IdentityMode SharedLab`; los recursos PDP por
   UUID exacto; el stub de fallos; la matriz Python. Ver [IDENTITY_DECISION](IDENTITY_DECISION.md#qué-es-exclusivo-del-laboratorio).
5. **Qué pedir a Seguridad-UCO antes de producción:**
   - el IdP/issuer único de confianza por ambiente (¿realm institucional en el PDP, o federación hacia la
     plataforma?) y su configuración HTTPS;
   - la fuente autoritativa y el aprovisionamiento automatizado del claim `idUsuario` (alta, cambio, baja,
     auditoría), sin asociación manual;
   - la política de audiencias (token con `security-baseline-pdp` + `asistencias-api`, o token exchange);
   - el modelo de recursos para rutas con `{grupoId}` (corrección upstream de plantillas o alternativa) y si el PDP
     debe ver parámetros como `grupoId`;
   - corrección upstream de PEP-03 (200 sin decisión) y del vínculo `environment` ↔ aplicación;
   - aprobación contractual del código `ACCESS_DECISION_UNAVAILABLE` (503);
   - flujo de login del frontend (Authorization Code + PKCE; sin ROPC) y gestión de la credencial de aplicación
     del PEP (rotación, secret manager).
6. **Acciones manuales pendientes:** ninguna para reproducir el laboratorio (todo está automatizado y descrito en
   la [MANUAL_TEST_GUIDE](MANUAL_TEST_GUIDE.md)). Pendientes de decisión humana: (a) aprobar o descartar la tarea
   separada para corregir el 500 de SSE (contrato 403); (b) decidir si se crea una DB E2E aislada o un login de solo
   lectura (requiere autorización explícita para modificar SQL Server); (c) las decisiones institucionales del punto 5.

## Estado al cerrar la sesión

- Rama `jose-valencia/experiment-security-baseline-mvc`, sin merge; `develop` intacto; frontend intacto.
- Backend 8083 detenido; laboratorio Compose arriba (PEP reiniciado y `UP`); Keycloak 8081, backend 8080 y SQL
  Server sin cambios (0 escrituras verificadas).
- Secretos solo en `%USERPROFILE%\.asistencias-security-lab\lab.env`; evidencias sin secretos en [evidence/](evidence/).

## Rollback

1. Backend: no arrancar 8083 o arrancarlo sin `-IdentityMode SharedLab` (por defecto `Institutional`). El flag PEP
   sigue OFF en `application.yml`.
2. Keycloak 19090 / PDP: pasos de [KEYCLOAK_LAB_CONFIGURATION §Retirada](KEYCLOAK_LAB_CONFIGURATION.md#retirada-rollback).
3. Contrato: revertir el commit de `docs/contracts/**` de esta rama.
4. Nada que revertir en SQL Server, Keycloak 8081 ni backend 8080.
