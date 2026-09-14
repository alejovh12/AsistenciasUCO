# Arquitectura de Runtime Security (independiente del proveedor)

Este documento describe cómo AsistenciasUCO valida y autoriza requests HTTP sin que la capa
de seguridad conozca la estructura específica de los tokens de Keycloak. Complementa
`docs/architecture/adapter-composition-standard.md` (que cubre el Composition Root general).

**Alcance de este documento**: únicamente *runtime authentication/authorization* (validar un
JWT ya emitido y resolver quién es el usuario y qué roles tiene). El *provisioning*
administrativo de usuarios (crear/verificar/eliminar usuarios en Keycloak, service account con
`client_credentials`, client roles y compensación DB↔Keycloak) es un tema distinto, cubierto por
`IdentityProviderPort`/`KeycloakIdentityProviderAdapter`, y no se toca aquí.

## 1. Runtime authentication vs. identity provisioning

Son dos capabilities distintas que hoy comparten el mismo IdP (Keycloak) pero no tienen por
qué evolucionar juntas:

| | Runtime authentication | Identity provisioning |
| --- | --- | --- |
| Pregunta que responde | ¿Quién hizo este request y qué puede hacer? | ¿Cómo se crea/gestiona una cuenta en el IdP? |
| Port/SPI | `JwtClaimsAdapter` | `IdentityProviderPort` |
| Adapter Keycloak | `KeycloakJwtClaimsAdapter` | `KeycloakIdentityProviderAdapter` |
| Selector | `app.adapters.security.provider` | `app.adapters.identity.provider` |
| Properties | `app.providers.keycloak-security.*` | `app.providers.keycloak-identity.*` |
| Se ejecuta | En cada request autenticado | Al crear/provisionar un usuario |

Antes de este prompt, `SecurityConfig` mezclaba ambas responsabilidades al leer
`app.security.keycloak.api-client-id` directamente. Ya no: `SecurityConfig` no importa nada
de `app.providers.keycloak.*` (admin) ni de `app.providers.keycloak-security.*` directamente;
solo depende de los beans neutrales `JwtDecoder` y `JwtClaimsAdapter`.

## 2. Flujo end-to-end

```text
HTTP Request (Authorization: Bearer <jwt>)
        ↓
Spring Security (BearerTokenAuthenticationFilter)
        ↓
JwtDecoder                              ← valida firma + issuer + audience + claim idUsuario
        ↓
InstitutionalJwtAuthenticationConverter ← SecurityConfig, neutral
        ↓
JwtClaimsAdapter                        ← SPI de infraestructura, neutral
        ↓
KeycloakJwtClaimsAdapter                ← ÚNICA clase que conoce Keycloak
        ↓
JwtAuthenticationToken(principal = idUsuario.toString(), authorities = ROLE_*)
        ↓
SecurityContextHolder
        ↓
AuthenticatedUserProvider.requireAuthenticatedUserId() ← lee Authentication.getName(), nada más
```

`SecurityConfig` conoce: `JwtDecoder`, `InstitutionalJwtAuthenticationConverter`, CORS,
`AuthenticationEntryPoint`/`AccessDeniedHandler`. No conoce Keycloak.

## 3. `JwtClaimsAdapter`

SPI de infraestructura (`infrastructure.adapter.primary.security.spi.JwtClaimsAdapter`):

```java
public interface JwtClaimsAdapter {
    UUID requireUserId(Jwt jwt);
    Set<InstitutionalRole> extractRoles(Jwt jwt);
}
```

Conoce `Jwt` (Spring Security) porque eso es infraestructura; no conoce ningún proveedor
concreto ni lógica de negocio de Application. Deliberadamente **no** se llama `KeycloakPort`,
`Auth0Port` ni similar — el contrato representa una capability ("dame el usuario y sus roles a
partir de un JWT"), no una tecnología.

## 4. `KeycloakJwtClaimsAdapter`

Única clase de todo el runtime de seguridad que conoce cómo Keycloak estructura sus claims:

- **idUsuario**: lee el claim configurado (`app.providers.keycloak-security.user-id-claim`,
  default `idUsuario`). Si está ausente, vacío o no es UUID, lanza
  `OAuth2AuthenticationException` (se traduce a 401 genérico por `ApiAuthenticationEntryPoint`,
  nunca un 500).
- **roles**: lee exclusivamente `resource_access[apiClientId].roles`, donde `apiClientId` viene
  de `app.providers.keycloak-security.api-client-id` (default `asistencias-api`).
  Deliberadamente **no** lee `realm_access` ni combina roles de otros clients (`account`, el
  client de frontend, etc.) — solo los roles del client de la API cuentan para autorización.
- **parsing defensivo**: cada nivel (`resource_access`, la entrada del client, `roles`, cada
  rol individual) se valida con `instanceof` antes de castear. Un token mal formado nunca
  produce `ClassCastException` — en el peor caso, `extractRoles` devuelve un `Set` vacío.
- **mapping token→rol exacto y controlado**: solo se reconocen los 5 nombres institucionales
  **literales** (`ADMINISTRADOR`, `DECANO`, `COORDINADOR`, `DOCENTE`, `ESTUDIANTE`). NO se
  aceptan códigos cortos (`AD`, `DE`, `CD`, `DO`, `ES`), NO se acepta el prefijo `ROLE_`
  (`ROLE_DOCENTE` no es un rol de token válido, es una authority de Spring), y NO hay
  normalización de mayúsculas/minúsculas — el mapping es una comparación exacta contra el
  conjunto de nombres válidos, nunca `contains`/`startsWith`/fuzzy matching. Cualquier otro
  string (roles técnicos de Keycloak como `offline_access`, `uma_authorization`, `account`,
  `default-roles-*`, o cualquier rol ajeno) se ignora en silencio — nunca lanza error por un
  rol adicional desconocido.

## 5. `InstitutionalJwtAuthenticationConverter`

Vive en `infrastructure.adapter.primary.security` (no en `.keycloak`, porque es neutral).
Responsabilidad única: `Jwt` → (vía `JwtClaimsAdapter`) → `UUID idUsuario` + `Set<InstitutionalRole>`
→ `GrantedAuthority` → `JwtAuthenticationToken(jwt, authorities, idUsuario.toString())`.

Depende de la interfaz `JwtClaimsAdapter`, nunca de `KeycloakJwtClaimsAdapter` directamente —
Spring inyecta lo que el Composition Root de seguridad haya registrado.

**Este converter es el ÚNICO lugar de todo el runtime de seguridad donde existe la convención
`ROLE_` de Spring Security** (`new SimpleGrantedAuthority("ROLE_" + role.name())`).
`InstitutionalRole` (Application, `application.security.InstitutionalRole`) es un enum puro —
solo las 5 constantes, sin métodos, sin `authority()`, sin mapping de token, sin ningún import
de Spring/JWT/Keycloak. `KeycloakJwtClaimsAdapter` hace la traducción token→rol (string exacto
→ `InstitutionalRole`); este converter hace la traducción rol→authority
(`InstitutionalRole`→`ROLE_*`). Ninguna de las dos responsabilidades vive en Application.

## 6. `idUsuario` como principal — sin contradicción `sub`

Antes existía una contradicción real: una constante `PRINCIPAL_CLAIM_NAME = "idUsuario"`
(declarada pero nunca usada) convivía con `converter.setPrincipalClaimName("sub")` (la que
realmente se ejecutaba). El principal resultante era el `sub` de Keycloak, no el UUID
institucional.

Ahora el principal es **siempre** `idUsuario.toString()`, sin excepción y sin fallback a
`sub`, `email` ni `preferred_username`. `AuthenticatedUserProvider` (ver más abajo) confía en
esa normalización: solo lee `Authentication.getName()` y lo parsea como UUID.

## 7. Roles de client, no roles de realm

Autorización de la API usa exclusivamente `resource_access[asistencias-api].roles`. Un rol que
solo exista en `realm_access.roles` (p.ej. asignado directamente en el realm en lugar del
client) **no** otorga ninguna autoridad. Esto es intencional: evita que roles de otro contexto
(p.ej. roles de administración de Keycloak en el realm) se filtren como autorización de la API.

## 8. Selección de provider

```yaml
app:
  adapters:
    security:
      provider: ${APP_ADAPTERS_SECURITY_PROVIDER:keycloak}   # YA EXISTÍA — reutilizado, no duplicado

  providers:
    keycloak-security:
      issuer-uri: ${KEYCLOAK_ISSUER_URI:http://127.0.0.1:8081/realms/asistencias-uco}
      api-client-id: ${KEYCLOAK_API_CLIENT_ID:asistencias-api}
      expected-audience: ${KEYCLOAK_EXPECTED_AUDIENCE:asistencias-api}
      user-id-claim: ${KEYCLOAK_USER_ID_CLAIM:idUsuario}
```

**`KEYCLOAK_ISSUER_URI` es la única fuente de verdad para el issuer.** Debe coincidir
**exactamente** con el claim `iss` emitido por Keycloak — la validación de issuer usa
`JwtValidators.createDefaultWithIssuer(...)` (comparación exacta, sin `endsWith`/`contains`).
Este documento, `application.yml` y `.env.example` usan `127.0.0.1` como ejemplo local por
consistencia entre sí; eso no cambia el issuer real de ningún ambiente, que siempre viene de
la variable de entorno.

`KeycloakSecurityAdapterConfiguration` (bajo `infrastructure.config.adapters.security.keycloak`)
registra, condicionado a `app.adapters.security.provider=keycloak`:

- `KeycloakSecurityProviderProperties` (fail-fast: las 4 properties son `@NotNull`/no-blank).
- `JwtClaimsAdapter` = `KeycloakJwtClaimsAdapter`.
- `JwtDecoder`, con el validador compuesto (ver sección 9).

**No** registra reglas HTTP ni el `InstitutionalJwtAuthenticationConverter` — ambos son
neutrales y viven en `SecurityConfig`.

**Ownership del `JwtDecoder`**: el bean productivo `jwtDecoder(...)` de
`KeycloakSecurityAdapterConfiguration` NO lleva `@ConditionalOnMissingBean`. Cuando
`app.adapters.security.provider=keycloak`, esta clase es la propietaria inequívoca del decoder
y de su política de validación (issuer + audience + claim UUID requerido) — ningún otro bean
puede sustituirlo silenciosamente y saltarse esa política. Los tests que necesitan un
`JwtDecoder` de prueba lo hacen sin cargar esta clase (slices `@WebMvcTest` con `@Import`
explícito, como `SecurityConfigTest`/`RbacSecurityFilterChainTest`), o habilitando
`spring.main.allow-bean-definition-overriding=true` únicamente en su propio contexto de
prueba (`KeycloakSecurityAdapterConfigurationTest`) — nunca debilitando la configuración
productiva.

## 9. Validadores JWT neutros

`infrastructure.adapter.primary.security.validation`:

- **`AudienceValidator`**: exige que `aud` contenga la audiencia esperada. Rechaza `aud` nulo,
  vacío, o que solo contenga otra audiencia (p.ej. `account` o el client de frontend) como
  sustituto.
- **`RequiredUuidClaimValidator`**: reutilizable — recibe el nombre del claim por constructor,
  no conoce `idUsuario` como valor hardcodeado. Exige que el claim exista, tenga texto y sea un
  UUID válido.

El `JwtDecoder` de Keycloak compone, vía `DelegatingOAuth2TokenValidator`:

```text
JwtValidators.createDefaultWithIssuer(issuerUri)   // issuer exacto, sin endsWith/contains
+ AudienceValidator(expectedAudience)
+ RequiredUuidClaimValidator(userIdClaim)
```

El issuer se valida con el validador estándar de Spring (comparación exacta), nunca con
`endsWith`/`contains`/regex parcial.

## 10. `AuthenticatedUserProvider`

Responsabilidad reducida a una sola línea conceptual:

```java
UUID requireAuthenticatedUserId();
```

Fuente: `SecurityContextHolder` → `Authentication.getName()` → `UUID.fromString(...)`. No lee
claims de `Jwt`, no conoce el nombre del claim `idUsuario`, no conoce Keycloak, no consulta la
base de datos, no busca por email ni `preferred_username`, no hace fallback a `sub`. Confía en
que `InstitutionalJwtAuthenticationConverter` ya dejó el principal normalizado.

## 11. Cómo agregar otro runtime IdP

Ejemplo: agregar Auth0 como proveedor de runtime security (no de provisioning):

1. Implementar `JwtClaimsAdapter` en `Auth0JwtClaimsAdapter` (bajo
   `infrastructure.adapter.primary.security.auth0`), interpretando la forma en que Auth0
   estructura sus claims/roles.
2. Crear `Auth0SecurityProviderProperties` (issuer, audience, claim de usuario, lo que Auth0
   requiera) bajo `infrastructure.config.properties.providers`.
3. Crear `Auth0SecurityAdapterConfiguration` condicionada a
   `app.adapters.security.provider=auth0`, registrando `JwtClaimsAdapter` y `JwtDecoder`.
4. Agregar `AUTH0` a `SecurityAdapterProperties.Provider`.
5. Configurar `app.adapters.security.provider=auth0` y sus properties.

`SecurityConfig`, `InstitutionalJwtAuthenticationConverter`, `AuthenticatedUserProvider`,
`AudienceValidator` y `RequiredUuidClaimValidator` **no cambian**. Solo se agrega un nuevo
adapter + su configuración condicional — igual que con cualquier otra capability del
Composition Root.

## 12. Deuda explícita (fuera de alcance aquí)

- Provisioning administrativo (`KeycloakIdentityProviderAdapter`) ya usa service account con
  `client_credentials`, verifica cuentas existentes y asegura client roles del client
  `asistencias-api`. La implementación estática está disponible; falta validación runtime/E2E
  contra Keycloak real.
- El contrato de `IdentityProviderPort` ya quedó endurecido en la Microfase 2B.2-A: el rol
  institucional cruza el Port como `InstitutionalRole` (nunca `String rolInstitucional` /
  `asignarRol(String, String)`), y `CuentaIdentidadDTO` ya no transporta `mensaje` ni `username`
  — ver `docs/security/keycloak-identity-provider.md` §7. Pendiente: la integración de ese Port
  con decano/coordinador (ver estado real en `keycloak-identity-provider.md`).
- RBAC revisado en este prompt (ver más abajo) documenta dudas puntuales sobre contratos
  funcionales no explícitos; no inventa políticas nuevas donde el contrato no está claro.

## 13. RBAC revisado

Se revisaron las familias de rutas indicadas: `/admin/**`, `/decano/**`, `/coordinador/**`,
`/docente/**`, `/docentes/**`, `/estudiante/**`, `/grupos/**`, `/sesiones/**`,
`/asistencias/**`, `/realtime/**`, `/usuarios/**`. Cambios y hallazgos:

| Ruta | Antes | Ahora | Razón |
| --- | --- | --- | --- |
| `/api/v1/docentes/**` (directorio general, `DocenteController`) | Sin regla propia → caía en `authenticated()` (cualquier rol, incluido ESTUDIANTE, podía crear/asignar docentes) | `hasAnyRole("COORDINADOR","ADMINISTRADOR")` | Gap real. **Duda documentada**: no hay contrato funcional explícito de quién administra este directorio; se asumió el mismo criterio que ya administra docentes en `/coordinador/docentes`. |
| `GET /api/v1/grupos/**` | `authenticated()` | `hasAnyRole("DOCENTE","COORDINADOR","ADMINISTRADOR")` | El docente necesita consultar sus grupos/estudiantes/reportes; no hay evidencia de que ESTUDIANTE deba verlos por grupo completo. |
| `POST/PUT/DELETE /api/v1/grupos/**` | `authenticated()` | `hasAnyRole("COORDINADOR","ADMINISTRADOR")` | Crear/actualizar grupo y matricular estudiante son commands de coordinación (Bloque 29 del prompt). |
| `GET /api/v1/sesiones/*/qr-token` | `authenticated()` | `hasRole("DOCENTE")` | Generar el QR/PIN es una acción del docente que dicta la sesión (hoy retorna `FeatureUnavailableException`, sin impacto funcional inmediato). |
| `POST /api/v1/asistencias/consultas/grupo` (legacy) | `authenticated()` | `hasAnyRole("DOCENTE","COORDINADOR","ADMINISTRADOR")` | Mismo criterio que su equivalente `GET /api/v1/grupos/{id}/asistencias`. |
| `POST /api/v1/realtime/emit` | `authenticated()` (cualquier usuario podía forzar un evento SSE a todos los suscriptores) | `hasRole("ADMINISTRADOR")` | Utilidad de desarrollo sin rol funcional propio. **No se eliminó** (fuera de alcance: el transporte SSE no se refactoriza en este prompt) — ver deuda para cuando Realtime tenga su propio Port. |
| `POST /api/v1/usuarios` | `authenticated()` | Sin cambio (ya no era `permitAll`) | **Riesgo documentado, no resuelto**: cualquier rol autenticado puede provisionar un usuario nuevo. No se identificó un contrato funcional explícito que indique qué rol debería exigirse (¿ADMINISTRADOR? ¿COORDINADOR?), y el prompt prohíbe inventar una política nueva o un autorregistro público. Debe aclararse en Prompt 2B junto con el resto de provisioning. |
| `/api/v1/estudiantes/**` (directorio general, plural, `EstudianteController`) | `authenticated()` | `hasAnyRole("COORDINADOR","ADMINISTRADOR")` | **Resuelto en el prompt correctivo.** `EstudianteController` expone `GET /api/v1/estudiantes` (listado filtrable por tipo de identificación, número, nombre, correo, institución, facultad, programa, **grupo**) y `GET /api/v1/estudiantes/{id}` (detalle de *cualquier* estudiante) — es un directorio institucional, no una vista acotada al propio estudiante ni a un docente. El docente ya tiene su vertical específica y acotada por grupo (`GET /api/v1/grupos/{grupoId}/estudiantes`, ya protegida), así que este directorio general no se abre a DOCENTE. No se cambió ningún contrato HTTP ni se inventaron rutas nuevas. |
| `/api/v1/archivos/**` | `authenticated()` | Sin cambio — **SECURITY DEBT — STORAGE PHASE** | Ver sección 15. No se decide una política de ownership/contexto ahora porque depende de introducir `FileStoragePort`; mantenerlo en `authenticated()` es una decisión temporal explícita, no un descuido. |
| `POST /api/v1/asistencias`, `/lote`, `/revisiones`; `/sesiones/**` (POST/PUT/PATCH); `/docente/**`; `/estudiante/**`; `/admin/**`; `/decano/**`; `/coordinador/**` | Ya protegidos correctamente | Sin cambio | Verificado contra Bloques 30-31 del prompt: coinciden con las reglas ya existentes. |

Ninguna de las reglas nuevas fue inventada libremente: donde el contrato funcional era
ambiguo, se documentó la duda en vez de asumir una política. `/api/v1/estudiantes/**` se
resolvió en el prompt correctivo (sección 13, ver política arriba) porque el código funcional
respaldaba una restricción clara; `/api/v1/archivos/**` sigue sin modificarse (ver sección 15).

## 14. Authorization layers

Dos capas de autorización, deliberadamente separadas:

```text
Layer 1 — coarse-grained:
  Spring Security / roles / route access (SecurityConfig, authorizeHttpRequests).
  Responde: "¿este rol puede llegar a esta ruta/método HTTP, en general?"

Layer 2 — contextual:
  Application UseCase + InstitutionalScopePort.
  Responde: "¿este usuario específico puede actuar sobre ESTE recurso específico
  (este grupo, esta sesión, este estudiante)?"
```

`SecurityConfig` resuelve exclusivamente Layer 1. Nunca debe intentar resolver Layer 2 (por
eso, p.ej., "¿este docente puede actuar sobre este grupo?" vive en Application vía
`InstitutionalScopePort`, no en un matcher de `SecurityConfig` — ver Prompt 2A, sección 30 del
prompt original). Un endpoint puede pasar Layer 1 (tiene el rol correcto) y aun así deber ser
rechazado en Layer 2 (no tiene scope sobre ese recurso concreto).

**Endpoints pendientes de Layer 2** (Layer 1 ya correcto, ver sección 16):

- `GET /api/v1/sesiones/{sesionId}` (`SesionController`): recibe solo el UUID de la sesión: no
  recibe el actor autenticado ni consulta `InstitutionalScopePort` para verificar que el
  docente/estudiante que consulta tenga relación con esa sesión.
- `GET /api/v1/sesiones/grupo/{grupoId}` (`SesionController`): mismo patrón — hoy además está
  sin implementar (`FeatureUnavailableException`), pero cuando se implemente necesitará el
  mismo scope contextual.
- `GET /api/v1/docentes/{docenteId}` y `GET /api/v1/docentes/{docenteId}/asignaciones`
  (`DocenteController`): reciben solo el UUID del docente; no hay verificación de que el
  llamador (p.ej. un COORDINADOR) tenga scope institucional sobre ese docente específico.
- `GET /api/v1/estudiantes/{estudianteId}` (`EstudianteController`): mismo patrón — solo UUID,
  sin scope institucional sobre ese estudiante específico.

Este prompt correctivo **no** implementa Layer 2 para estos endpoints (sería un refactor de
autorización contextual fuera de alcance) — solo los lista y los deja marcados para la fase de
"authorization hardening". No se agregaron comprobaciones improvisadas en ningún Controller ni
se intentó resolver scope desde `SecurityConfig`.

## 15. `/api/v1/archivos/**` — SECURITY DEBT — STORAGE PHASE

`ArchivoController` (`POST /subir`, `GET /{nombreArchivo}`) permanece en `authenticated()`:
cualquier usuario autenticado puede subir o descargar cualquier archivo por nombre. No hay hoy:

- Un `FileStoragePort`/`GuardarArchivoInputPort` (ver deuda ya documentada en
  `docs/architecture/adapter-composition-standard.md`) a través del cual aplicar una regla de
  autorización coherente.
- Una noción de *ownership* (¿quién subió el archivo? ¿a qué solicitud de revisión pertenece?)
  ni de contexto institucional (¿el docente que descarga pertenece al grupo de la solicitud?)
  disponible en el Controller o en un Port.

Definir una política de roles ahora, sin ese contexto, sería o demasiado laxa (no resuelve el
riesgo real) o inventar un contrato de ownership que no existe todavía en el código. Por eso
se mantiene `authenticated()` **temporalmente**, marcado explícitamente como:

```text
SECURITY DEBT — STORAGE PHASE
```

Debe resolverse junto con la introducción de `FileStoragePort` (ver
`docs/architecture/adapter-composition-standard.md`, sección "Ejemplo: Local Storage → MinIO"):
la autorización por ownership/contexto de archivos es Layer 2 (sección 14), no una regla nueva
de `SecurityConfig`.
