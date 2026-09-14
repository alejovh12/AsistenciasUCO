# Identity Provisioning — Keycloak Admin API

Este documento describe cómo AsistenciasUCO provisiona cuentas institucionales en Keycloak
mediante su Admin REST API. Complementa
`docs/security/runtime-security-provider-architecture.md` (que cubre *runtime authentication*,
una capability distinta) y `docs/architecture/adapter-composition-standard.md` (Composition
Root general).

**Alcance**: el adapter `IdentityProviderPort` → `KeycloakIdentityProviderAdapter` y su conexión
con la Admin REST API, el contrato tipado de `IdentityProviderPort` endurecido en la Microfase
2B.2-A (Identity Contract Hardening), y la integración DB-first de
`RegistrarEstudianteUseCaseImpl` con este Port (Microfase 2B.2-B), y la integración DB-first de
`CrearDecanoUseCaseImpl` (Microfase 2B.2-C1). `CrearCoordinadorUseCase` todavía no llama a
Identity — ver sección "Deuda
explícita" al final.

## 1. `IdentityProviderPort`

Vive en `application.secondaryports.identity`. Expresa **capabilities**, no operaciones
técnicas de Keycloak:

```java
public interface IdentityProviderPort {
    CuentaIdentidadDTO crearCuenta(CrearCuentaIdentidadDTO dto);
    void eliminarCuenta(String idExterno);
    void asignarRol(String idExterno, InstitutionalRole rol);
}
```

Neutral desde su origen (no menciona Keycloak, realm, admin token ni HTTP) y, desde 2B.2-A,
también tipado: el rol institucional cruza el Port como `InstitutionalRole` — Application nunca
puede enviar un rol arbitrario como `String`. Deliberadamente **no** existe
`KeycloakPort`/`KeycloakAdminPort`/`RealmPort`.

`eliminarCuenta` es compensación **best-effort** para una identidad creada durante la misma
operación de `crearCuenta` (ver sección 3.5) — no es un mecanismo de rollback distribuido entre
la base de datos institucional y el IdP. La reconciliación DB↔IdP para una operación de DB que
ya hizo commit y falla después en Identity es una capability de una fase posterior.

## 2. Modelo de entrada y resultado

`CrearCuentaIdentidadDTO` (`application.secondaryports.identity.dto`) contiene únicamente
información institucional neutral — username, `idUsuario`, correo, nombre, apellido, password
inicial, rol institucional tipado — sin `realm`, `clientId`, `clientSecret` ni ningún detalle de
Keycloak:

```java
public record CrearCuentaIdentidadDTO(
        String username, UUID idUsuario, String correo, String primerNombre, String primerApellido,
        String passwordInicial, InstitutionalRole rolInstitucional) {}
```

`CuentaIdentidadDTO` es el resultado neutral de Identity: únicamente `idExterno` +
`newlyCreated`. Desde 2B.2-A ya **no** transporta `username` ni `mensaje` — Application no debe
depender de un mensaje humano generado por el IdP para construir su propia respuesta funcional
(ver sección 4 de la deuda resuelta más abajo).

```java
public record CuentaIdentidadDTO(String idExterno, boolean newlyCreated) {}
```

- `newlyCreated=true` → el adapter creó la identidad en esta operación.
- `newlyCreated=false` → la identidad ya existía y fue verificada exactamente (username +
  email + atributo `idUsuario`, los tres exactos).

### Rol institucional tipado de punta a punta

`InstitutionalRole` (`application.security`) es el único enum de roles institucionales — no se
duplicó ni se creó un `KeycloakRole` paralelo. `ProvisionarUsuarioIdentityMapper` construye
`CrearCuentaIdentidadDTO` pasando el enum directamente (`InstitutionalRole.ESTUDIANTE` en
`ProvisionarUsuarioUseCaseImpl`, ya tipado — ver deuda explícita). El adapter ya no ejecuta
`InstitutionalRole.valueOf(String)`: como Application solo puede enviar el enum, la traducción
se reduce a `rolInstitucional.name()` tras validar no-nulo
(`requireValidInstitutionalRoleName`). Nunca códigos cortos, nunca alias, nunca prefijo
`ROLE_`.

## 3. `KeycloakIdentityProviderAdapter`

Única clase de Identity Provisioning que conoce Keycloak. Flujo:

```text
Application
    ↓
IdentityProviderPort
    ↓
KeycloakIdentityProviderAdapter
    ↓
Keycloak Admin REST API
```

### 3.1 `client_credentials` (nunca password grant)

```text
POST {serverUrl}/realms/{realm}/protocol/openid-connect/token
grant_type=client_credentials
client_id={adminClientId}
client_secret={adminClientSecret}
```

Antes existía autenticación administrativa basada en credenciales de usuario contra
`/realms/master/...` (admin-cli). Ahora: `client_credentials` contra el **realm de la
aplicación** (no `master`) usando un client confidencial de *service account*
(`asistencias-backend-admin`, ver
`docs/security/keycloak-service-account.md`). No existe fallback a password grant.

La respuesta se parsea con Jackson a un record (`access_token`), nunca con regex ni
concatenación manual. Se valida: HTTP 2xx, `access_token` presente y no vacío. El secret y el
token **nunca** aparecen en mensajes de excepción ni en logs.

### 3.2 Creación de usuario

`POST {serverUrl}/admin/realms/{realm}/users` con payload construido con un record Jackson:

```json
{
  "username": "...", "email": "...", "firstName": "...", "lastName": "...",
  "enabled": true, "emailVerified": true,
  "attributes": { "idUsuario": ["<UUID recibido de Application>"] }
}
```

El nombre del atributo sale de `userIdAttribute` (configurable, default `idUsuario`) — nunca
hardcodeado. El UUID es exactamente el recibido desde Application (`dbo.Usuario.id`); el
adapter nunca genera otro.

La implementación actual marca `emailVerified=true` porque supone que el correo institucional
recibido desde Application ya es confiable y está verificado. Esta decisión funcional debe
confirmarse antes de producción.

- **Búsqueda previa**: `GET /admin/realms/{realm}/users?q={userIdAttribute}:{idUsuario}`
  (parámetro `q` codificado). `idUsuario` es el vínculo institucional estable. Cero resultados
  permite crear; uno exige coincidencia exacta del atributo, username y correo canónico de DB;
  más de uno falla por ambigüedad. Un username o correo distinto produce un fallo controlado,
  sin crear otra cuenta ni modificar automáticamente la existente. La reconciliación de ese
  drift queda pendiente para una fase explícita.
- **HTTP 201**: el `externalId` se resuelve preferentemente desde el header `Location` — nunca
  se asume `externalId == idUsuario` (son identificadores distintos). Si `Location` falta o no
  identifica de forma válida al usuario creado, se busca por `idUsuario` y se verifican
  username, email e `idUsuario` antes de continuar como `newlyCreated=true`. Sin un único
  resultado válido se lanza `IdentityProviderException`; no se elimina una identidad cuyo
  `externalId` no se conoce de forma segura.
- **HTTP 409**: se repite la búsqueda por `idUsuario` para cubrir la carrera entre el pre-check
  y el POST. Solo una identidad que coincide exactamente en atributo, username y correo se
  reutiliza (`newlyCreated=false`), sin resetear password. Cero resultados, ambigüedad o drift
  son `IdentityProviderException`; nunca se presupone éxito por el 409.

### 3.3 Password (solo cuenta nueva)

`PUT /admin/realms/{realm}/users/{externalId}/reset-password` con
`{"type":"password","value":"...","temporary":false}` — **solo** cuando `newlyCreated=true`.
Una cuenta existente nunca ve modificada su credencial por este flujo.

### 3.4 Client role (nunca realm role)

Runtime security lee `resource_access["asistencias-api"].roles` (ver
`docs/security/runtime-security-provider-architecture.md`). Por eso el rol institucional se
asigna como **client role** del client `apiClientId`, nunca como realm role:

1. `GET /admin/realms/{realm}/clients?clientId={apiClientId}` → exactamente un resultado, se
   toma su `id` interno (UUID del client). Ninguno, más de uno, `id` vacío o `clientId`
   distinto del solicitado es error.
2. `GET /admin/realms/{realm}/clients/{clientUuid}/roles/{roleName}` → la representación EXACTA
   que entrega Keycloak, parseada a un record interno tipado. Se exige `id` no vacío y
   `name == roleName`; si Keycloak devuelve otro role, no se asigna silenciosamente.
3. `GET /admin/realms/{realm}/users/{externalId}/role-mappings/clients/{clientUuid}` → consulta
   los client roles ya asignados para evitar depender de que el POST tolere duplicados.
4. Si falta el role, `POST /admin/realms/{realm}/users/{externalId}/role-mappings/clients/{clientUuid}`
   con esa misma representación tipada, sin reconstruir IDs.

`roleName` es siempre `rolInstitucional.name()` — la traducción está centralizada en un único
método (`requireValidInstitutionalRoleName`), nunca duplicada como una cadena de `if`. Desde
2B.2-A ya no existe conversión `InstitutionalRole.valueOf(String)` en este adapter: Application
solo puede enviar el enum, así que aquí solo se valida no-nulo. Nunca códigos cortos (`DO`,
`ES`...), nunca alias, nunca prefijo `ROLE_` (eso es una convención de Spring Security que solo
existe en `InstitutionalJwtAuthenticationConverter`, del lado de runtime security).

El client role se asegura tanto para cuentas nuevas como para cuentas existentes verificadas.
La semántica de `crearCuenta(...)` es "garantizar la identidad institucional solicitada":
una cuenta existente válida (`username` + `email` + `idUsuario`) retorna `newlyCreated=false`,
pero antes debe quedar con el client role institucional solicitado. En ese camino no se
resetea password, no se modifica `idUsuario` y nunca se elimina la cuenta.

### 3.5 Compensación

Si, con `newlyCreated=true`, falla el password o el client-role mapping:

```text
DELETE /admin/realms/{realm}/users/{externalId}
```

Best-effort: nunca lanza. Si el DELETE también falla, se registra `IDENTITY_COMPENSATION_FAILED`
(con `correlationId`, `externalUserId`, `provider=keycloak`, sin secretos) y se propaga la
excepción **original** de provisioning — nunca la del fallo de compensación.

La compensación interna reutiliza el `adminToken` ya obtenido al inicio de `crearCuenta(...)`;
no pide un segundo token para el DELETE compensatorio. Si `newlyCreated=false`, esta rama de
compensación es estructuralmente inalcanzable: aunque falle el ensure del client role, se
propaga `IdentityProviderException` sin resetear password y sin eliminar una cuenta
preexistente.

## 4. Logging

Permitido y usado: `correlationId`, `realm`, `apiClientId`, `externalUserId`, `operation`,
`stage`, HTTP status. Prohibido y nunca presente: `adminClientSecret`, el access token
administrativo, passwords de usuario, el header `Authorization`, el payload de credenciales.

## 5. Properties y Composition Root

```yaml
app:
  adapters:
    identity:
      provider: ${APP_ADAPTERS_IDENTITY_PROVIDER:keycloak}   # ya existía — reutilizado

  providers:
    keycloak-identity:
      server-url: ${KEYCLOAK_SERVER_URL:http://127.0.0.1:8081}
      realm: ${KEYCLOAK_REALM:asistencias-uco}
      admin-client-id: ${KEYCLOAK_ADMIN_CLIENT_ID:asistencias-backend-admin}
      admin-client-secret: ${KEYCLOAK_ADMIN_CLIENT_SECRET:}
      api-client-id: ${KEYCLOAK_API_CLIENT_ID:asistencias-api}
      user-id-attribute: ${KEYCLOAK_USER_ID_ATTRIBUTE:idUsuario}
```

`admin-client-secret` no tiene default funcional (no `change-me`/`admin`/`secret`/`password`):
si el provider de identity es Keycloak y falta el secret, el arranque falla
(`KeycloakIdentityProviderProperties` lo valida fail-fast en su constructor compacto).

`KeycloakIdentityAdapterConfiguration` (`infrastructure.config.adapters.identity.keycloak`),
condicionada a `app.adapters.identity.provider=keycloak`, es el único lugar que conecta
`IdentityProviderPort` → `KeycloakIdentityProviderAdapter`. El adapter no se autoregistra con
`@Component`/`@Service`/`@Repository`. Esta configuración no registra nada de runtime security.

La implementación estática del adapter y su Composition Root están disponibles y cubiertos por
tests de backend/fake HTTP. La validación contra un Keycloak real de ambiente sigue pendiente
de runtime/E2E.

## 6. Cómo agregar otro proveedor de identidad

1. Implementar `IdentityProviderPort` en un nuevo adapter.
2. Crear sus properties tipadas bajo `app.providers.<nuevo>`.
3. Crear una configuración equivalente a `KeycloakIdentityAdapterConfiguration`, condicionada
   a `app.adapters.identity.provider=<nuevo>`.
4. Agregar el nuevo valor a `IdentityAdapterProperties.Provider`.

`ProvisionarUsuarioUseCase`, `CrearUsuarioUseCase` y los controllers no cambian. Los UseCases que
ya dependen de `IdentityProviderPort` (`ProvisionarUsuarioUseCaseImpl`,
`RegistrarEstudianteUseCaseImpl`) tampoco requieren cambios — siguen dependiendo únicamente de la
interfaz de Application.

## 7. Registrar Estudiante en Grupo — integración DB-first (2B.2-B)

`RegistrarEstudianteUseCaseImpl` (`application.features.grupo.registrarestudianteengrupo`)
provisiona identidad institucional para el estudiante recién registrado, con esta secuencia
estricta:

```text
validaciones + deteccion de conflicto de identidad (pre-command, sin cambios)
        ↓
resolver semantica de password para DB (hash o null, sin cambios)
        ↓
GrupoRepositoryPort.registrarEstudianteEnGrupo(...)   ← DB COMMAND
        ↓
DB SUCCESS
        ↓
UsuarioRepositoryPort.consultarUsuarioPorIdentificacion(tipoIdentificacionId, numeroIdentificacion)
        ↓
idUsuario y correo canonico resueltos POST-COMMAND desde dbo.uv_usuario
        ↓
IdentityProviderPort.crearCuenta(...)   ← InstitutionalRole.ESTUDIANTE
        ↓
resultado funcional (mensaje de DB, sin semantica del IdP)
```

Puntos de diseño:

- **DB-first real, no solo en el nombre**: `IdentityProviderPort` nunca se invoca antes de que
  `GrupoRepositoryPort.registrarEstudianteEnGrupo(...)` retorne exitosamente. Un fallo de DB
  (excepción de `GrupoRepositoryPort`) se propaga sin llamar a Identity.
- **UUID y correo canónicos por identificación**: el SP actualiza
  `tipoIdIdentificacion`/`numeroIdentificacion`/nombres/apellidos de un usuario preexistente
  encontrado por documento, pero no necesariamente su correo — por eso la referencia
  post-command confiable es `tipoIdentificacionId + numeroIdentificacion`, nunca el correo del
  request. La proyección leída de `dbo.uv_usuario` entrega `id` y `correo`; ambos pasan al
  mapper de Identity. Así, un request con correo nuevo no puede provisionar ese correo si la
  DB conservó el anterior. Las
  consultas pre-command por correo **y** por identificación se mantienen intactas (detectan
  conflicto correo→usuario A / documento→usuario B antes de ejecutar el SP).
- **Inconsistencia técnica post-commit**: si el SP tuvo éxito pero
  `consultarUsuarioPorIdentificacion(...)` no encuentra al usuario, es un error técnico
  (`InternalApplicationException`, mensaje neutral, sin mencionar Identity/Keycloak) — nunca un
  404 funcional, nunca un intento de reparar la DB, nunca una llamada a Identity.
- **Password raw vs. hash, nunca confundidos**: la contraseña que llega a
  `PasswordEncoderPort`/DB es siempre distinta del valor que llega a `IdentityProviderPort`.
  Usuario DB nuevo: `PasswordEncoderPort.encode(...)` produce el hash para el SP; el raw original
  (`domain.getPassword()`) es lo que se envía a Identity. Usuario DB existente: el password para
  persistir sigue siendo `null` (no se toca el password DB); Identity recibe igualmente el raw
  disponible (puede ser `null` si el request no lo trajo) — nunca el hash, nunca inventado. Si
  ese raw es insuficiente para una identidad de IdP que resulta no existir, el fallo lo produce
  el propio `KeycloakIdentityProviderAdapter` (rechaza password inválido/nulo) y se propaga como
  el mismo error controlado de Identity-tras-DB-success — esta microfase no añade recuperación de
  contraseña.
- **Rol determinado por el servidor**: `RegistrarEstudianteUseCaseImpl` es específico a
  "registrar estudiante" — no puede producir otro rol funcionalmente, así que fija
  `InstitutionalRole.ESTUDIANTE` internamente (constante tipada). El request HTTP no puede influir
  en el rol.
- **Fallo de Identity tras DB success**: se captura `IdentityProviderPort.IdentityProviderException`
  y se transforma en `InternalApplicationException` (con `cause` preservada) — la DB permanece
  confirmada, no hay rollback, no se invoca `eliminarCuenta`. La reconciliación distribuida
  DB↔IdP es una capability de una fase posterior.
- **Mapper propio de la feature**: `RegistrarEstudianteIdentityMapper`
  (`usecase.mapper`) construye el `CrearCuentaIdentidadDTO` a partir del Domain — no reutiliza
  `ProvisionarUsuarioIdentityMapper` (evita acoplar dominios de features distintas).
- **Composition Root**: `GrupoBeansConfig` inyecta `IdentityProviderPort` (la interfaz de
  Application, nunca `KeycloakIdentityProviderAdapter`) en el bean de
  `RegistrarEstudianteUseCase`.

## 8. Deuda explícita

Desde 2B.2-C1.2, `CrearDecanoUseCaseImpl` también es DB-first: valida Facultad y consulta
`dbo.uv_usuario` por correo normalizado y por documento compuesto (`tipoIdentificacionId` +
`numeroIdentificacion`). Solo trata al usuario como nuevo cuando ambas consultas no encuentran
nada, o como preexistente cuando ambas devuelven el mismo UUID. Una resolución parcial o dos
UUID distintos producen `ERR_IDENTIDAD_USUARIO_CONFLICTO` antes del command; la misma regla de
DB se traduce a HTTP 409 si una carrera llega al SP. Para un usuario nuevo, envía a DB el hash
de `PasswordEncoderPort`; para uno existente, envía password `null`. Tras el command, relee el
perfil canónico completo de `dbo.uv_usuario`: por `idUsuario` preexistente o, si era nuevo, por
documento compuesto y verifica el correo normalizado. `CrearDecanoIdentityMapper` usa el UUID,
número, correo, primer nombre y primer apellido canónicos, conserva el raw password del request
y fija `InstitutionalRole.DECANO`. Un fallo posterior del IdP no intenta rollback de DB ni llama
a `eliminarCuenta` desde Application.

Resuelto en la Microfase 2B.2-A (Identity Contract Hardening) — ya no es deuda:

- `CrearCuentaIdentidadDTO.rolInstitucional` es `InstitutionalRole` (antes `String`).
- `IdentityProviderPort.asignarRol` recibe `InstitutionalRole` (antes `String nombreRol`).
- `CuentaIdentidadDTO` ya no transporta `username` ni `mensaje` — solo `idExterno` +
  `newlyCreated`.
- `KeycloakIdentityProviderAdapter` ya no ejecuta `InstitutionalRole.valueOf(String)`; solo
  `rol.name()` tras validar no-nulo.
- `ProvisionarUsuarioIdentityMapper` ya no construye el mensaje funcional concatenando
  `cuenta.mensaje()` — el mensaje devuelto a Application/API es exclusivamente el de la
  creación del usuario en la base de datos institucional.
- La documentación de `eliminarCuenta` ya no sugiere un rollback distribuido DB↔IdP; se aclaró
  que es compensación best-effort dentro de la misma operación de `crearCuenta`.

Resuelto en la Microfase 2B.2-B (Registrar Estudiante → DB → IdentityProviderPort):

- `RegistrarEstudianteUseCaseImpl` (`grupo/registrarestudianteengrupo`) ahora **sí** provisiona
  identidad — DB-first, siempre como `InstitutionalRole.ESTUDIANTE` (determinado por el
  servidor, nunca por el request HTTP). Ver sección 7.

Estado real de qué flujo provisiona identidad, a la fecha:

| UseCase | ¿Llama a `IdentityProviderPort`? | Rol |
|---|---|---|
| `ProvisionarUsuarioUseCaseImpl` (`/api/v1/usuarios`) | Sí | Siempre `ESTUDIANTE` (hardcode pendiente — ver 2B.2-D abajo) |
| `RegistrarEstudianteUseCaseImpl` (registrar estudiante en grupo) | Sí (desde 2B.2-B) | Siempre `ESTUDIANTE` (correcto: esta feature no puede producir otro rol) |
| `CrearDecanoUseCaseImpl` | Sí (desde 2B.2-C1) | Siempre `DECANO`, determinado por el servidor |
| `CrearCoordinadorUseCaseImpl` | No | — |
| `RegistrarDocenteDesdeUsuarioUseCaseImpl` | No | — |

Pendiente — fuera de alcance de 2B.2-A/2B.2-B, para **2B.2-D** y fases posteriores:

- `ProvisionarUsuarioUseCaseImpl` sigue pasando siempre `InstitutionalRole.ESTUDIANTE` — ahora
  tipado, pero el hardcode funcional **no está resuelto**: el UseCase genérico de
  `/api/v1/usuarios` sigue sin integrarse con
  `CrearDecanoUseCase`/`CrearCoordinadorUseCase`/`RegistrarDocenteDesdeUsuarioUseCase`. A
  diferencia de `RegistrarEstudianteUseCaseImpl` (donde ESTUDIANTE es la única semántica
  funcionalmente posible para esa feature), en `ProvisionarUsuarioUseCaseImpl` sigue siendo un
  hardcode porque ese endpoint es genérico y podría en teoría provisionar cualquier rol.
- No hay compensación DB→Keycloak ni Keycloak→DB coordinada a nivel de UseCase: si el command de
  DB tiene éxito pero `identityProviderPort.crearCuenta(...)` falla, el UseCase transforma el
  fallo en un error controlado de Application (`InternalApplicationException`) y lo relanza — la
  DB permanece confirmada, nunca se hace rollback ficticio ni se invoca
  `eliminarCuenta` como compensación distribuida. La reconciliación DB↔IdP para ese caso es una
  capability de una fase posterior.
