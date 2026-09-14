# Configuración externa requerida en Keycloak — Service Account administrativo

Este documento describe la configuración que debe existir **manualmente** en el Keycloak
real para que `KeycloakIdentityProviderAdapter` funcione. AsistenciasUCO **no modifica
Keycloak automáticamente**: esta configuración se hace una vez, fuera del backend, por un
administrador de Keycloak.

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
| Consultar usuarios (búsqueda por username exacto tras un 409) | `GET /users` | `view-users` |
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

No requiere configuración especial de "user profile" en Keycloak para que el adapter
funcione: los `attributes` arbitrarios se pueden escribir vía Admin API sin declarar el
atributo de antemano. Si el realm tiene habilitado un "Unmanaged attributes" restrictivo en el
User Profile (Keycloak 24+), debe permitir escribir el atributo `idUsuario` (o el nombre
configurado en `KEYCLOAK_USER_ID_ATTRIBUTE`) vía Admin API — de lo contrario Keycloak podría
ignorar o rechazar ese atributo silenciosamente al crear el usuario.

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
`docs/security/runtime-security-provider-architecture.md`, sección 19 (aspectos externos de
Keycloak) del reporte del prompt de Runtime Security.

La implementación estática del adapter espera esta configuración externa, pero este documento
no afirma validación E2E contra un Keycloak real.
