# Estándar de Composition Root y selección de adapters

Este documento describe cómo AsistenciasUCO selecciona la tecnología detrás de cada
capability (persistencia, identidad, seguridad, almacenamiento, tiempo real, auditoría) y
cómo incorporar una tecnología nueva sin tocar Domain ni Application.

## 1. Objetivo del Composition Root

Domain y Application definen **qué** capability necesitan (a través de un Port) pero nunca
**qué tecnología** la implementa. Esa decisión se toma una sola vez, al iniciar Spring, en un
conjunto de clases `@Configuration` agrupadas bajo `infrastructure.config.adapters.*`: el
**Composition Root**.

```
                    DOMAIN
                       ▲
                       │
                  APPLICATION
                       │
              Secondary Port
                       │
        ┌──────────────┼──────────────┐
        │              │              │
        ▼              ▼              ▼
   Adapter A       Adapter B       Adapter C
   Tecnología 1    Tecnología 2    Local/NoOp
        │              │              │
        └──────────────┼──────────────┘
                       │
                COMPOSITION ROOT
                       │
                CONFIGURACIÓN
```

Application, Domain y los Controllers dependen únicamente del Port. Ninguno de ellos sabe
si detrás hay SQL Server, Keycloak, almacenamiento local o cualquier otra tecnología.

Regla general de dirección de dependencias:

```text
Primary Adapter -> InputPort
Application UseCase -> SecondaryPort
Secondary Adapter -> SecondaryPort
```

## 2. Diferencia entre Port y Adapter

- **Port**: interfaz en `application.secondaryports.*` que expresa una *capability*
  (`GrupoRepositoryPort`, `IdentityProviderPort`, `InstitutionalScopePort`...). Nunca lleva el
  nombre de una tecnología.
- **Adapter**: implementación concreta de un Port para una tecnología específica
  (`GrupoRepositorySqlServerAdapter`, `KeycloakIdentityProviderAdapter`). Vive en
  `infrastructure.adapter.secondary.*` y no se autoregistra con `@Repository`, `@Component` ni
  `@Service`: quien decide instanciarlo es el Composition Root.

## 3. PROFILE vs PROVIDER

Estos dos conceptos se mezclaban antes de este prompt y ahora están separados:

| Concepto  | Responde a...              | Mecanismo                                   |
|-----------|-----------------------------|----------------------------------------------|
| PROFILE   | ¿En qué **entorno** corro? (local, test, staging, prod) | `spring.profiles.active` |
| PROVIDER  | ¿Con qué **tecnología** cumplo una capability? | `app.adapters.<capability>.provider` + `@ConditionalOnProperty` |

**Antes** (incorrecto): `@Profile("mock")`, `@Profile("!mock")` decidían la tecnología del
repositorio de Sesión/Asistencia. Un profile de entorno terminaba seleccionando tecnología.

**Ahora**: la tecnología se selecciona exclusivamente con `app.adapters.*.provider`, evaluado
por `@ConditionalOnProperty` en las clases del Composition Root. El profile de Spring solo
describe el entorno y no participa en la selección de adapters.

## 4. Estructura `app.adapters.*`

Selecciona **qué** adapter implementa cada capability:

```yaml
app:
  adapters:
    persistence:
      provider: ${APP_ADAPTERS_PERSISTENCE_PROVIDER:sqlserver}
    identity:
      provider: ${APP_ADAPTERS_IDENTITY_PROVIDER:keycloak}
    security:
      provider: ${APP_ADAPTERS_SECURITY_PROVIDER:keycloak}
    storage:
      provider: ${APP_ADAPTERS_STORAGE_PROVIDER:local}
    realtime:
      provider: ${APP_ADAPTERS_REALTIME_PROVIDER:local-sse}
    audit:
      provider: ${APP_ADAPTERS_AUDIT_PROVIDER:logging}
```

Cada valor está tipado en `infrastructure.config.properties.adapters` mediante
`@ConfigurationProperties` (nunca `@Value`), con un enum `Provider` por capability y
validación de "no nulo" en el constructor compacto del record.

## 5. Estructura `app.providers.*`

Configura **cómo** se conecta la tecnología ya elegida (no decide cuál usar):

```yaml
app:
  providers:
    keycloak-identity:
      server-url: ...
      realm: ...
      admin-client-id: ...
      admin-client-secret: ...
      api-client-id: ...
      user-id-attribute: ...
    keycloak-security:
      issuer-uri: ...
      api-client-id: ...
      expected-audience: ...
      user-id-claim: ...
    local-storage:
      upload-directory: ...
```

Tipado en `infrastructure.config.properties.providers` (p.ej.
`KeycloakIdentityProviderProperties` y `KeycloakSecurityProviderProperties`).

No mezclar responsabilidades: `app.adapters.identity.provider=keycloak` dice *qué* adapter
usar; `app.providers.keycloak-identity.*` dice *cómo* se conecta el adapter administrativo.
Runtime security usa su propio bloque `app.providers.keycloak-security.*`.

## 6. Cómo agregar un nuevo adapter

Ejemplo: agregar un nuevo proveedor de identidad `auth0` sin tocar nada de Application:

1. Implementar `IdentityProviderPort` en un nuevo adapter (`infrastructure.adapter.secondary.identity`).
2. Crear `Auth0ProviderProperties` bajo `infrastructure.config.properties.providers`.
3. Crear `Auth0IdentityAdapterConfiguration` bajo `infrastructure.config.adapters.identity.auth0`,
   con `@ConditionalOnProperty(prefix = "app.adapters.identity", name = "provider", havingValue = "auth0")`.
4. Agregar `AUTH0` al enum `IdentityAdapterProperties.Provider`.
5. Configurar `app.adapters.identity.provider=auth0` y las properties de `app.providers.auth0.*`.
6. Agregar pruebas del nuevo adapter.

**Nunca** se debe:
- Modificar `UseCase`, `Interactor` o `InputPort`.
- Modificar `Controller`.
- Modificar Domain.
- Agregar un `if`/`switch` sobre el nombre del provider en Application o en el UseCase.

## 7. Capas que NO cambian al sustituir tecnología

Al cambiar de tecnología para una capability, estas capas permanecen intactas:

- Domain (`usecase.domain`)
- Application (`UseCase`, `Interactor`, `InputPort`, `SecondaryPort`)
- Controllers (`infrastructure.adapter.primary.controller`)

Solo cambian: el adapter concreto, sus properties tipadas y la clase de Composition Root que
lo registra.

## 8. Fail-fast

`@ConditionalOnProperty(..., matchIfMissing = true)` solo aplica al valor por defecto actual
(`sqlserver`, `keycloak`, `local`, `local-sse`, `logging`). Si se configura un provider que no
tiene una configuración condicional que lo satisfaga (p.ej. `app.adapters.identity.provider=auth0`
sin haber creado `Auth0IdentityAdapterConfiguration`), Spring simplemente no encuentra un bean
para el Port correspondiente y el contexto falla al iniciar (`UnsatisfiedDependencyException`).
No existe fallback silencioso a Keycloak o SQL Server.

En esta fase se conserva `matchIfMissing = true` por compatibilidad con configuraciones
existentes. Una vez todos los ambientes declaren `app.adapters.*.provider` explicitamente,
deberia evaluarse eliminar `matchIfMissing` para maximizar el fail-fast.

## 9. Ejemplo: Keycloak → otro IdP

Ver sección 6. El único punto de cambio es `KeycloakIdentityAdapterConfiguration` (o su
equivalente para el nuevo IdP) y `app.adapters.identity.provider`. `IdentityProviderPort`,
`ProvisionarUsuarioUseCase`, `CrearUsuarioUseCase` y los controllers no se tocan.

## 10. Ejemplo: Local Storage → MinIO

Hoy `ArchivoController` accede al filesystem local directamente (no hay todavía un
`GuardarArchivoInputPort` ni un `FileStoragePort`); por eso este prompt solo introdujo
`app.adapters.storage.provider=local` y `app.providers.local-storage.upload-directory` sin
refactorizar el controller. Cuando se introduzca `FileStoragePort`, el flujo correcto es:

```text
ArchivoController
        ↓
GuardarArchivoInputPort
        ↓
GuardarArchivoInteractor
        ↓
GuardarArchivoUseCase
        ↓
FileStoragePort
        ↓
LocalFileStorageAdapter / MinioFileStorageAdapter
```

Pasos esperados:

1. Crear el caso de uso de Application (`GuardarArchivoUseCase`) y su `GuardarArchivoInputPort`.
2. Cambiar `ArchivoController` para depender del `GuardarArchivoInputPort`, nunca del `FileStoragePort`.
3. Extraer la lógica de almacenamiento a `LocalFileStorageAdapter implements FileStoragePort`.
4. Crear `MinioFileStorageAdapter implements FileStoragePort` + `MinioProviderProperties`.
5. Crear `MinioStorageAdapterConfiguration` condicionada a `app.adapters.storage.provider=minio`.

## 11. Ejemplo: SQL Server → otra persistencia

1. Implementar cada `*RepositoryPort` / `*QueryPort` / `*CommandPort` en adapters de la nueva
   tecnología (p.ej. `GrupoRepositoryPostgresAdapter`).
2. Crear las configuraciones equivalentes a `SqlServerCoreRepositoryAdapterConfiguration`,
   `SqlServerAcademicAdapterConfiguration`, `SqlServerSecurityScopeAdapterConfiguration` y
   `SqlServerReportAdapterConfiguration`, condicionadas a
   `app.adapters.persistence.provider=postgres`.
3. Agregar `POSTGRES` a `PersistenceAdapterProperties.Provider`.
4. Las Feature Configs (`GrupoBeansConfig`, etc.) no cambian: solo conocen el Port.

## 12. Observabilidad como estándar OTel, no ports por vendor

Observabilidad (trazas, métricas, logs) se basa en OpenTelemetry, Micrometer y logging
estructurado con exportación OTLP. Deliberadamente **no** existen `GrafanaPort`, `LokiPort`,
`TempoPort` ni `PrometheusPort`: el backend de observabilidad se selecciona externamente, vía
configuración del collector, no desde código Java.

## 13. Source/package consistency

La estructura de directorios bajo `src/main/java` debe reflejar el `package` Java declarado
por cada archivo. Por ejemplo, una clase con:

```java
package co.edu.uco.asistenciasuco.application.features.coordinador.common.dto;
```

debe vivir bajo:

```text
src/main/java/co/edu/uco/asistenciasuco/application/features/coordinador/common/dto/
```

No se aceptan archivos que compilen por casualidad aunque su ruta fisica contradiga el
package declarado. El test `SourcePackageConsistencyTest` protege esta regla.

## 14. Datasource ownership

Estado actual:

- SQL Server es el unico provider de persistencia implementado.
- `spring.datasource.*` continua siendo configuracion global de Spring Boot.
- `spring.datasource.driver-class-name` es configurable mediante
  `${SPRING_DATASOURCE_DRIVER_CLASS_NAME:com.microsoft.sqlserver.jdbc.SQLServerDriver}`.

Estado objetivo antes de incorporar una segunda persistencia:

- `SqlServerDataSourceConfiguration` debera crear el `DataSource` de manera condicional cuando
  `app.adapters.persistence.provider=sqlserver`.
- La configuracion propia de SQL Server se movera a `app.providers.sqlserver.*`.
- Un provider no JDBC no debe obligar a crear `DataSource`.

## 15. Matriz actual de capabilities

| Capability | Port | Current Adapter | Provider property | Replaceable today? | Remaining work |
| ---------- | ---- | --------------- | ----------------- | ------------------ | -------------- |
| Persistence | `*RepositoryPort`, academic `*QueryPort`/`*CommandPort`, `InstitutionalScopePort` | SQL Server adapters bajo `infrastructure.adapter.secondary.*` | `app.adapters.persistence.provider=sqlserver` | Parcialmente, por ports; solo SQL Server implementado | DataSource provider-specific antes de incorporar otra DB |
| Identity Provisioning | `IdentityProviderPort` | `KeycloakIdentityProviderAdapter` | `app.adapters.identity.provider=keycloak` | Sí, por port; solo Keycloak implementado — protocolo admin ya es `client_credentials`/service account (no password grant), roles institucionales como client roles del client `asistencias-api`; implementación estática disponible, pendiente runtime/E2E | Integrar el port con `CrearDecanoUseCase`/`CrearCoordinadorUseCase`/registro de docentes (hoy solo el flujo de estudiante provisiona identidad); ver `docs/security/keycloak-identity-provider.md` sección 7 |
| Runtime Security | `JwtClaimsAdapter` (SPI, `infrastructure.adapter.primary.security.spi`) | `KeycloakJwtClaimsAdapter` | `app.adapters.security.provider` | Sí, por SPI; solo Keycloak implementado — no es "multi-provider" hoy | Validación runtime/E2E y deudas de autorización contextual; ver `docs/security/runtime-security-provider-architecture.md` |
| Password Encoding | `PasswordEncoderPort` | `SpringPasswordEncoderAdapter` | Configuracion de adapter password actual | Si | Sin deuda de provider conocida en este bloque |
| Storage | Pendiente `GuardarArchivoInputPort`/`FileStoragePort` | Filesystem directo en `ArchivoController` | `app.adapters.storage.provider=local` preparado | No | Introducir InputPort, UseCase y `FileStoragePort` |
| Realtime | Pendiente `RealtimePublisherPort`/`EventPublisherPort` | SSE local en `RealtimeEventHub` | `app.adapters.realtime.provider=local-sse` preparado | No | Extraer port y adapter de publicacion |
| Audit | `AuditEventPublisher` | `LoggingAuditEventPublisher` + soporte SQL opcional | `app.adapters.audit.provider=logging` | Parcialmente; logging funciona sin repositorio SQL | Separar providers si se requiere auditoria durable dedicada |
| Observability | APIs estandar OTel/Micrometer/logging | OpenTelemetry, Micrometer, logging estructurado | Configuracion Spring/OTLP externa | Si, por configuracion externa | Validacion runtime/operacional |

## 16. Regla contra Service Locator

Prohibido:

```java
Map<String, IdentityProviderPort> providersPorNombre; // NO
AdapterFactory.get(provider); // NO
```

y prohibido que Application consulte `Environment`, `ApplicationContext` o
`@ConfigurationProperties` para decidir en tiempo de ejecución qué implementación usar. La
resolución del provider ocurre una sola vez, al construir el contexto de Spring.

## 17. Regla contra switch/provider dentro de UseCases

Prohibido:

```java
if (provider.equals("keycloak")) { ... }
switch (provider) { ... }
new KeycloakIdentityProviderAdapter(...); // dentro de un UseCase o Feature Config
```

Un `UseCase` o `Interactor` que necesite ramificar por tecnología es una señal de que el Port
está mal diseñado (expone detalles de la tecnología) o de que la selección se está haciendo en
la capa equivocada.

## 18. Deuda conocida (siguiente bloque Identity provisioning — Prompt 2B.2)

Runtime security (`SecurityConfig`, `JwtClaimsAdapter`, `KeycloakJwtClaimsAdapter`,
`InstitutionalJwtAuthenticationConverter`, validadores JWT neutros) quedó completamente
desacoplado de Keycloak en el Prompt 2A — ver
`docs/security/runtime-security-provider-architecture.md` para el detalle. Identity
Provisioning (`KeycloakIdentityProviderAdapter`) ya usa `client_credentials` contra un service
account, verifica cuentas existentes y asegura client roles de `asistencias-api` (no password
grant, no realm roles) — ver
`docs/security/keycloak-identity-provider.md` y `docs/security/keycloak-service-account.md`.
Pendiente:

1. Integrar `IdentityProviderPort` con los casos de uso que hoy no lo llaman
   (`CrearDecanoUseCase`, `CrearCoordinadorUseCase`, registro de docentes) — hoy solo
   `ProvisionarUsuarioUseCaseImpl` (flujo de estudiante) provisiona identidad en Keycloak; ver
   deuda detallada en `docs/security/keycloak-identity-provider.md` sección 7.
2. Revisar provisioning institucional para que las reglas de alcance y datos de identidad sean
   consistentes entre SQL Server y Keycloak (compensación DB↔Keycloak a nivel de UseCase).
3. `ArchivoController` sigue accediendo al filesystem local directamente, sin un
  `GuardarArchivoInputPort`/`FileStoragePort`. El correctivo pendiente debe introducir el
  InputPort antes del SecondaryPort; el controller nunca debe depender del `FileStoragePort`.
4. `RealtimeEventHub`/SSE no están detrás de un Port todavía. `app.adapters.realtime.provider=local-sse`
  está preparado para cuando se introduzca un `RealtimePublisherPort`. El endpoint
  `POST /api/v1/realtime/emit` es una utilidad de desarrollo sin rol funcional propio; se
  restringió a `ADMINISTRADOR` en el Prompt 2A como medida provisional (ver documento de
  runtime security).
5. El `DataSource` debe pasar a ser provider-specific antes de incorporar una segunda DB.
6. Los mocks de repositorio (`*RepositoryMockAdapter`) permanecen en `src/main/java` (no se
  movieron a `src/test/java`) para minimizar el impacto. Ya no se autoregistran ni se
  seleccionan por profile: los tests que los necesiten deben importarlos explícitamente (ver
  `MockRepositoryTestConfiguration` en `src/test/java/.../infrastructure/config`).
7. Directorios generales sin contrato de rol claro, restringidos con criterio conservador en
   el Prompt 2A (`/api/v1/docentes/**` → `COORDINADOR`/`ADMINISTRADOR`) y directorios
   equivalentes aún sin revisar (`/api/v1/estudiantes/**`, plural): ver duda documentada en
   `docs/security/runtime-security-provider-architecture.md`.
8. Validar en runtime/CI el Composition Root completo.
