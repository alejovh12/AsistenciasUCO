# Configuración Keycloak del laboratorio (19090)

**Solo laboratorio.** Realm `security-baseline` del Compose `asistencias-security-lab`. El Keycloak institucional
(8081, realm `asistencias-uco`) no se tocó. Toda la configuración la aplica, de forma idempotente,
[`provision_shared_identity_e2e.py`](../../../infra/security-baseline-lab/scripts/provision_shared_identity_e2e.py)
con la Admin REST API; los secretos se generan aleatoriamente y se guardan solo en
`%USERPROFILE%\.asistencias-security-lab\lab.env` (claves `LAB_E2E_CLIENT_SECRET`,
`LAB_E2E_WRONG_AUD_CLIENT_SECRET`, `LAB_USER_LAB_E2E_*_PASSWORD`). Ninguna contraseña del Keycloak institucional
se reutiliza.

## Estado previo (lectura 2026-10-14)

- Realm mínimo: `defaultDefaultClientScopes` vacío, sin scope `roles` (no hay `resource_access`), User Profile con
  solo `username/email/firstName/lastName`.
- Scope `security-baseline-claims` (usado por el PDP): `pdp-audience` (`security-baseline-pdp`),
  `subject-from-keycloak-id` (`sub` = id Keycloak), `tenant` (hardcoded `universidad-uco`), `bff-audience`.
- Clientes del PDP: `security-baseline-bff`, `security-pep-internal`. **No se modificaron.**

## Cambios

| Objeto | Configuración | Motivo |
|---|---|---|
| User Profile | atributo `idUsuario`: patrón UUID, `view=[user,admin]`, `edit=[admin]`, monovalor, no requerido | idéntico al bootstrap institucional (`infra/keycloak/scripts/bootstrap-keycloak.ps1` §14) |
| Cliente `asistencias-api` | confidencial, sin flujos (standard/direct/implicit/service account OFF); client roles `DOCENTE`, `ESTUDIANTE` | namespace de roles que exige `KEYCLOAK_API_CLIENT_ID`; sin `ADMINISTRADOR` ni `COORDINADOR` |
| Cliente `asistencias-lab-e2e` | confidencial; **solo** Direct Access Grants (laboratorio); `fullScopeAllowed=false`; default scope único `security-baseline-claims`; scope mapping `asistencias-api: DOCENTE, ESTUDIANTE` | emisor del JWT compartido |
| └ mapper `idUsuario` | `oidc-usermodel-attribute-mapper`, `user.attribute=idUsuario`, `claim.name=idUsuario`, String, access token + introspection | claim requerido por el backend |
| └ mapper `asistencias-api-client-roles` | `oidc-usermodel-client-role-mapper`, `clientId=asistencias-api`, `claim.name=resource_access.${client_id}.roles`, multivalor | `resource_access.asistencias-api.roles` |
| └ mapper `audience-asistencias-api` | `oidc-audience-mapper`, `included.client.audience=asistencias-api`, access token | `aud` del backend |
| Cliente `asistencias-lab-e2e-wrong-aud` | igual que el anterior **sin** `audience-asistencias-api` | caso negativo de audiencia (única diferencia) |
| Usuarios `lab-e2e-*` | `emailVerified=true` (el PDP lo exige para aprovisionar), sin acciones requeridas, un único client role de `asistencias-api` | ver [IDENTITY_DECISION](IDENTITY_DECISION.md#procedencia-del-uuid) |

## Claims resultantes (sin imprimir el JWT)

`asistencias-lab-e2e` / `lab-e2e-docente-fixpag300`:

```json
{"alg":"RS256","kidInJwks":true,
 "iss":"http://keycloak.localhost:19090/realms/security-baseline",
 "aud":["asistencias-api","security-baseline-bff","security-baseline-pdp"],
 "azp":"asistencias-lab-e2e","roles":["DOCENTE"],
 "idUsuario":"de000003-0000-4000-8000-000000000001",
 "sub":"c05297af-5179-40bf-9898-fef166acd69d","subIsKeycloakId":true,"subDiffersFromIdUsuario":true,
 "lifetimeSeconds":300,"tenant":"universidad-uco"}
```

Las cinco combinaciones (cuatro usuarios + `wrong-aud`) están en [evidence/provisioning.json](evidence/provisioning.json).
`aud` incluye también `security-baseline-bff` porque lo añade el scope existente del PDP; no afecta al backend
(`AudienceValidator` exige que `aud` *contenga* `asistencias-api`).

## PDP (aplicación existente `asistencias-uco-lab`)

| Recurso GET exacto | Nuevo | Concedido a `DOCENTE_E2E_FIXPAG300_LAB` |
|---|---|---|
| `/api/v1/docente/horarios` | no (SEC-BASELINE-001) | sí |
| `/api/v1/realtime/stream` | no (SEC-BASELINE-001) | sí |
| `/api/v1/grupos/de000005-…-0001/estudiantes` | sí | sí |
| `/api/v2/sesiones/grupo/de000005-…-0001` | sí | sí |
| `/api/v1/grupos/de000005-…-0002/estudiantes` (ajeno) | sí | **sí, a propósito** |
| `/api/v2/sesiones/grupo/de000005-…-0002` (ajeno) | sí | **sí, a propósito** |

Asignaciones nuevas: `lab-e2e-docente-fixpag300`, `lab-e2e-rol-incorrecto`, `lab-e2e-sin-idusuario`.
`lab-e2e-docente-sin-pdp` no tiene asignación. El rol `DOCENTE_LECTURA_LAB`, su asignación y los recursos
existentes no se modificaron (conceder un recurso existente a un rol nuevo no altera el recurso). Sin wildcards y
sin rol administrativo para el docente. Resumen PDP tras el aprovisionamiento: 7 recursos, 3 roles, 5 asignaciones.

Las rutas exactas por UUID **no son una solución de autorización para grupos dinámicos**: el PDP v0.1.6 responde
500 a plantillas `{grupoId}`, una variante de mayúsculas del mismo UUID se deniega (E2E-14) y la query string no se
evalúa (SSE). Ver [SECURITY_CONTRACT_MATRIX](SECURITY_CONTRACT_MATRIX.md#limitaciones-de-autorización-pdp).

## Retirada (rollback)

En la consola de administración del 19090 (o Admin API): borrar los usuarios `lab-e2e-*`, los clientes
`asistencias-lab-e2e`, `asistencias-lab-e2e-wrong-aud` y `asistencias-api`, y el atributo `idUsuario` del User
Profile; en el PDP, el rol `DOCENTE_E2E_FIXPAG300_LAB` y los 4 recursos nuevos. Borrar las claves `LAB_E2E_*` /
`LAB_USER_LAB_E2E_*` de `lab.env`. No usar `docker compose down -v`.
