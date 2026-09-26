---
status: active
type: runbook
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

# Configuración externa requerida en Keycloak — Service Account administrativo

Este documento describe la configuración que debe existir en Keycloak para que
`KeycloakIdentityProviderAdapter` funcione. Esta configuración es **externa al backend**: el
backend de negocio nunca crea clients, scopes ni el realm en runtime (ver diferenciación más
abajo).

**En LOCAL/DEV esta configuración ya no se hace manualmente en Keycloak Admin Console.**
`infra/keycloak/scripts/bootstrap-keycloak.ps1` la crea/reconcilia de forma automatizada e
idempotente vía Admin REST API — ver `infra/keycloak/README.md` para el flujo completo
(instalación limpia vía `realm-import/asistencias-uco-realm.json` + reconciliación de un realm
existente vía el script). En otros ambientes (staging/producción), el equivalente de ese
bootstrap corre como paso administrativo de infraestructura, separado del despliegue del
backend.

## 0. Bootstrap de infraestructura vs provisioning funcional

No confundir estas dos capabilities, deliberadamente separadas:

- **Bootstrap de infraestructura** (`infra/keycloak/scripts/bootstrap-keycloak.ps1` y
  `realm-import/asistencias-uco-realm.json`): crea/reconcilia el realm, los 3 clients, los 5
  roles institucionales, el client scope `asistencias-api-scope` y sus mappers, y los permisos
  mínimos del service account descritos en este documento. Lo ejecuta un desarrollador/ops,
  fuera del backend.
- **Provisioning funcional del backend** (`IdentityProviderPort` →
  `KeycloakIdentityProviderAdapter`, descrito en el resto de este documento y en
  `docs/security/keycloak-identity-provider.md`): crea/verifica cuentas institucionales
  individuales en runtime, usando la infraestructura ya bootstrapeada. El backend nunca crea el
  client `asistencias-api`, el client scope, ni el propio realm — solo consume el service
  account `asistencias-backend-admin` ya configurado.

## 1. Client confidencial de service account

Debe existir, en el realm configurado (`app.providers.keycloak-identity.realm`, por defecto
`asistencias-uco`), un client:

```text
Client ID: asistencias-backend-admin   (o el valor configurado en KEYCLOAK_ADMIN_CLIENT_ID)
Client type: confidential (OpenID Connect, "Client authentication" = ON)
Standard flow: OFF (no se usa login interactivo)
Direct access grants: OFF (no se usa password grant)
Service accounts roles: ON
```

Este es el client contra el que el adapter obtiene el admin token vía `client_credentials`
(`POST /realms/{realm}/protocol/openid-connect/token`). El **secret** de este client es
`KEYCLOAK_ADMIN_CLIENT_SECRET` — debe guardarse como secreto de despliegue (variable de
entorno, vault del entorno de CI/CD, etc.), nunca en el repositorio.

## 2. Permisos mínimos del service account (no excesivos)

El adapter solo usa las siguientes operaciones de la Admin REST API — el service account debe
poder hacer exactamente esto, no más:

| Operación que usa el adapter | Endpoint | Permiso `realm-management` requerido |
| --- | --- | --- |
| Consultar usuarios (búsqueda por idUsuario y verificación exacta de identidad) | `GET /users` | `view-users` |
| Crear usuarios | `POST /users` | `manage-users` |
| Eliminar usuarios (compensación) | `DELETE /users/{id}` | `manage-users` |
| Actualizar credenciales de un usuario recién creado | `PUT /users/{id}/reset-password` | `manage-users` |
| Consultar clients (resolver `apiClientId`) | `GET /clients` | `view-clients` |
| Consultar client roles | `GET /clients/{id}/roles/{role-name}` | `view-clients` |
| Consultar client roles ya asignados al usuario | `GET /users/{id}/role-mappings/clients/{id}` | `view-users` |
| Asignar client roles a un usuario | `POST /users/{id}/role-mappings/clients/{id}` | `manage-users` |

En la práctica, en el realm de Keycloak esto se traduce en asignar al service account del
client `asistencias-backend-admin` (pestaña "Service account roles") los client roles
correspondientes del client `realm-management`:

```text
view-users
manage-users
view-clients
```

**No** se requiere: `manage-realm`, `manage-clients` (crear/modificar/eliminar clients —
el adapter solo *consulta* clients, nunca los crea ni modifica), `manage-authorization`,
`manage-events`, ni ningún permiso a nivel de `master` realm. El adapter jamás opera contra el
realm `master` (a diferencia del flujo password-grant/admin-cli anterior a este prompt, que sí
lo hacía).

## 3. Client de API cuyos roles se administran

El adapter asigna client roles del client `apiClientId` (por defecto `asistencias-api`, el
mismo client que Runtime Security usa para leer `resource_access["asistencias-api"].roles` —
ver `docs/security/runtime-security-provider-architecture.md`). Ese client debe tener
definidos, como client roles (no realm roles), exactamente los 5 roles institucionales:

```text
ADMINISTRADOR
DECANO
COORDINADOR
DOCENTE
ESTUDIANTE
```

Sin alias, sin códigos cortos, sin prefijo `ROLE_` — esos nombres literales son los que el
adapter busca vía `GET /clients/{id}/roles/{roleName}`.

## 4. Atributo de usuario `idUsuario`

El adapter escribe `attributes.idUsuario` vía Admin API. Si el realm tiene habilitado un
"Unmanaged attributes" restrictivo en el User Profile (Keycloak 24+), Keycloak podría ignorar o
rechazar ese atributo silenciosamente al crear el usuario — por eso `bootstrap-keycloak.ps1`
declara explícitamente el atributo `idUsuario` en el User Profile del realm (`view=user,admin`, `edit=admin`), en vez de depender de una política permisiva de unmanaged attributes.
Esto también evita que un usuario final pueda autoasignarse otro UUID institucional editando su
propio perfil. Ver `infra/keycloak/README.md` sección 1 para el flujo DB-first completo de
`idUsuario`.

## 5. Resumen de variables de entorno

```env
KEYCLOAK_SERVER_URL=http://127.0.0.1:8081
KEYCLOAK_REALM=asistencias-uco
KEYCLOAK_ADMIN_CLIENT_ID=asistencias-backend-admin
KEYCLOAK_ADMIN_CLIENT_SECRET=<secreto del client, fuera del repositorio>
KEYCLOAK_API_CLIENT_ID=asistencias-api
KEYCLOAK_USER_ID_ATTRIBUTE=idUsuario
```

## 6. Qué NO se documenta aquí

Esto cubre únicamente lo que el adapter de **provisioning** necesita. La configuración del
client de runtime security (audiencia del JWT, protocol mapper del claim `idUsuario` en el
token de acceso de usuarios finales) está fuera de alcance de este documento — ver
[runtime-security-provider-architecture](runtime-security-provider-architecture.md) y [bootstrap Keycloak](../../infra/keycloak/README.md), secciones 3 y 6.

La implementación estática del adapter espera esta configuración externa, pero este documento
no afirma validación E2E contra un Keycloak real.
