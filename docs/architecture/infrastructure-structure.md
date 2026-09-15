# Estructura de Infrastructure y Crosscutting

Este documento es la referencia oficial del árbol de `infrastructure` y `crosscutting` tras el
refactor estructural de `refactor/infrastructure-structure`. Es un refactor puro de
MOVE/RENAME/PACKAGE/IMPORT/CONFIG-ORGANIZATION/TEST-MIGRATION/DOCUMENTATION: ningún endpoint
HTTP, contrato JSON, stored procedure, regla de negocio o RBAC cambió.

## 1. Árbol oficial

```text
infrastructure/
  adapter/
    primary/                         # entrada externa al backend
      controller/                    # controllers REST por feature (admin, asistencia, ...)
      realtime/sse/
        controller/                  # RealtimeEventsController
        contract/                    # RealtimeStreamGateway (contrato interno, no Application Port)
        response/                    # RealtimeEventResponse (DTO HTTP/SSE)
        localsse/                    # LocalSseRealtimeStreamGateway (implementación local-sse)
      security/
        contract/                    # AuthenticatedUserResolver
        handler/                     # ApiAuthenticationEntryPoint, ApiAccessDeniedHandler,
                                      # SecurityErrorResponseWriter
        jwt/                         # InstitutionalJwtAuthenticationConverter,
                                      # SecurityContextAuthenticatedUserResolver
          contract/                  # JwtClaimsExtractor (SPI)
          keycloak/                  # KeycloakJwtClaimsExtractor
          validation/                # AudienceValidator, RequiredUuidClaimValidator
    secondary/                       # salidas del backend, agrupadas por capability -> provider
      persistence/sqlserver/
        core/                        # *RepositoryPort sobre SQL Server (asistencia, docente,
                                      # estudiante, grupo, sesion, tipoidentificacion, usuario)
        academic/                    # adapters de solo-lectura sobre catálogo académico
        reporting/                   # modelos de lectura especializados (ReporteAsistencia...)
        authorization/                # InstitutionalScopeSqlServerAdapter
        support/
          error/                     # DbExceptionTranslator, DbFailureClassifier, ...
          mapping/                   # row mappers, JdbcValueMapper
          procedure/                 # CanonicalStoredProcedureExecutor (ejecución de usp_*)
      identity/keycloak/             # KeycloakIdentityProviderAdapter
      realtime/localsse/             # ReactorRealtimeAdapter
      cryptography/password/spring/  # SpringPasswordEncoderAdapter
  audit/                             # ver sección 4 (no es observability)
    model/                           # AuditEvent, AuditActorType, AuditOutcome, RequestActor
    contract/                        # AuditEventPublisher
    adapter/
      logging/                      # LoggingAuditEventPublisher (siempre activo)
      sqlserver/                    # AuditEventJdbcRepository (ver deuda arquitectónica, §7)
    web/                             # AuditInterceptor, AuditRequestAttributes,
                                      # Audit{Request,Response}BodyCaptureAdvice, AuditableOperation
  observability/                     # telemetría de plataforma, no auditoría de negocio
    correlation/                     # CorrelationIdContext
    tracing/opentelemetry/           # TraceContextSnapshot
  config/
    adapters/                        # Composition Root: SELECCIÓN de tecnología por capability
      persistence/sqlserver/         # SqlServer*AdapterConfiguration (core, academic, reporting,
                                      # security scope, procedure support, audit support)
      identity/keycloak/             # KeycloakIdentityAdapterConfiguration
      security/{keycloak,password}/  # KeycloakSecurityAdapterConfiguration,
                                      # PasswordEncoderAdapterConfiguration
      realtime/localsse/             # LocalSseRealtimeAdapterConfiguration
      audit/                         # AuditAdapterConfiguration
    wiring/                          # Composition Root: ENSAMBLAJE de UseCase+Interactor a
                                      # partir de Application Ports, cero tecnología
                                      # (<Feature>WiringConfiguration, uno por feature)
    security/                        # SecurityConfig (filter chain de Spring Security)
    properties/{adapters,providers}/ # @ConfigurationProperties por capability y por provider
    jackson/                         # JacksonInputConfig
    AuditWebConfig.java              # registra AuditInterceptor en Spring MVC
crosscutting/
  exception/{,catalog}/              # CrosscuttingException, TechnicalException, ErrorDefinition,
                                      # ErrorKind, catálogos de error comunes/seguridad
  validation/                        # ValidationHelper, ValidationPatterns, ValidationErrorType,
                                      # ValidationIssue, ValidationResult(Builder), Validator
  sanitization/                      # SensitiveDataSanitizer
  util/                              # NumberHelper, ObjectHelper, TextHelper (genéricos, sin
                                      # responsabilidad de dominio ni técnica específica)
```

## 2. Regla de organización: capability -> provider -> implementación

Cada carpeta bajo `infrastructure/adapter/secondary` (y sus pares en `config/adapters`,
`audit/adapter`) sigue el mismo patrón de tres niveles:

1. **Capability**: la capacidad que Application necesita (`persistence`, `identity`, `realtime`,
   `cryptography`, `audit`). Nombra el "qué", nunca el "cómo".
2. **Provider**: la tecnología/proveedor concreto que satisface esa capability hoy
   (`sqlserver`, `keycloak`, `localsse`, `spring`, `logging`). Puede haber más de uno por
   capability (p. ej. `audit/adapter/{logging,sqlserver}`).
3. **Implementación**: la(s) clase(s) concreta(s) dentro de ese provider.

Este patrón es el que permite responder "¿cómo agrego Postgres/Auth0/Redis?" con una sola
respuesta: se agrega un nuevo paquete `provider` hermano bajo la misma `capability`, nunca se
reorganiza la capability existente.

## 3. Primary Adapter vs Secondary Adapter

- **Primary Adapter** (`infrastructure/adapter/primary`): puntos de entrada. Traducen un
  protocolo externo (HTTP, SSE) a una llamada a un Application primary port (`InputPort`).
  Nunca importan un adapter secundario concreto (`*SqlServerAdapter`, `KeycloakIdentityProviderAdapter`,
  `ReactorRealtimeAdapter`, ...); ArchUnit (`ControllersMustDependOnlyOnInputPortsTest`) lo
  garantiza.
- **Secondary Adapter** (`infrastructure/adapter/secondary`): puntos de salida. Implementan un
  Application secondary port (`*RepositoryPort`, `*QueryPort`, `IdentityProviderPort`,
  `RealtimePublisherPort`, `PasswordEncoderPort`, `InstitutionalScopePort`) contra una tecnología
  concreta.

## 4. Application Port vs contrato interno de Infrastructure

Dos categorías de interfaz, deliberadamente distintas:

- **Application Port** (`application/primaryports/*`, `application/secondaryports/*`): el
  contrato que Application expone o necesita. Vive siempre en Application, nunca se mueve a
  Infrastructure. Ejemplos: `AsistenciaRepositoryPort`, `IdentityProviderPort`,
  `RealtimePublisherPort`, `InstitutionalScopePort`, `PasswordEncoderPort`.
- **Contrato interno de Infrastructure** (`contract/` o `spi/` dentro de un paquete de
  Infrastructure): coordina colaboración *dentro* de Infrastructure, sin que Application lo
  conozca. Ejemplos: `RealtimeStreamGateway` (primary.realtime.sse.contract - conecta el
  controller SSE con la implementación local-sse sin que el controller importe
  `ReactorRealtimeAdapter`), `JwtClaimsExtractor` (primary.security.jwt.contract - aísla cómo un
  proveedor OIDC concreto estructura sus claims), `AuditEventPublisher` (audit.contract).

## 5. Convención de nombres

| Sufijo | Significado | Uso |
|---|---|---|
| `Port` | Contrato de Application | `AsistenciaRepositoryPort`, `IdentityProviderPort` |
| `Adapter` | Implementación técnica concreta de un Port o contrato interno | `AsistenciaRepositorySqlServerAdapter`, `KeycloakIdentityProviderAdapter`, `SpringPasswordEncoderAdapter`. **Nunca** como sufijo de interfaz (ver `InfrastructureStructureRulesTest.ninguna_interfaz_productiva_termina_en_adapter`) |
| `Gateway` | Frontera interna entre un primary adapter y el resto de Infrastructure | `RealtimeStreamGateway` |
| `Resolver` | Resuelve contexto/información a partir del entorno de ejecución | `AuthenticatedUserResolver`, `SecurityContextAuthenticatedUserResolver`, `RequestActorResolver` |
| `Extractor` | Extrae/interpreta datos de una estructura externa | `JwtClaimsExtractor`, `KeycloakJwtClaimsExtractor` |
| `Mapper` | Transforma entre representaciones (fila SQL, HTTP, etc.) | `GrupoRepositoryRowMapper`, `GrupoHttpMapper` |
| `Configuration` | Clase de wiring/composición | `AsistenciaWiringConfiguration`, `SqlServerCoreRepositoryAdapterConfiguration` |

`Impl` se evita siempre que exista un nombre semántico mejor; la única excepción tolerada es
dentro de `application/features/**/usecase/impl` (convención ya establecida y fuera del alcance
de este refactor).

## 6. Capability vs Provider: quién tiene Application Port y quién no

No toda tecnología externa necesita un Application Port. La regla:

- **Con Port/Adapter** cuando Application necesita genuinamente una capability externa que el
  negocio consume: persistencia SQL Server (`*RepositoryPort`), aprovisionamiento de identidad en
  Keycloak (`IdentityProviderPort`), publicación de eventos realtime (`RealtimePublisherPort`).
  Application programa contra el Port; Infrastructure decide el provider en el Composition Root.
- **Sin Port, solo estándar/SDK/config** cuando se trata de telemetría de plataforma transversal
  que Application ni siquiera debería saber que existe: métricas (Micrometer + exporter
  Prometheus + scrape), tracing (OpenTelemetry + OTLP + collector Tempo/Alloy), logs
  estructurados (Logback + Loki), dashboards (Grafana, consumidor puro, nunca productor). Estas
  piezas se configuran vía `application.yml`/`build.gradle`/`pom.xml`, nunca vía un Port de
  Application.

Ver también la sección 8.

## 7. Por qué Audit no es Observability

`AuditEvent` (quién hizo qué, cuándo, con qué resultado, para cumplimiento/trazabilidad de
negocio) es un concepto de negocio con reglas de sanitización (`SensitiveDataSanitizer`) y un
modelo persistente propio. No es telemetría de infraestructura. Antes de este refactor vivía
repartido entre `infrastructure.observability.audit` (modelo + contrato),
`infrastructure.adapter.secondary.audit` (publishers) e
`infrastructure.adapter.primary.controller.audit` (interceptor HTTP). Ahora vive unificado bajo
`infrastructure.audit`, con la misma estructura capability->provider que el resto de
Infrastructure (`adapter/{logging,sqlserver}`), separado explícitamente de
`infrastructure.observability` (correlation, tracing).

**DEUDA ARQUITECTÓNICA CONOCIDA**: `infrastructure.audit.adapter.sqlserver.AuditEventJdbcRepository`
ejecuta `INSERT`/`SELECT` directos contra `dbo.AuditoriaEvento` vía `JdbcTemplate`
(`NamedParameterJdbcTemplate.update(SQL_INSERT, ...)` / `JdbcTemplate.query(SQL_FIND_BY_CORRELATION_ID, ...)`),
sin pasar por un stored procedure `usp_*` público, violando la regla "nunca DML directo, solo
contratos `usp_*` públicos, nunca `usp_*_interno`" que el resto de la capa de persistencia
respeta. Este refactor **relocalizó la clase por organización, sin tocar su DML**: crear un
stored procedure nuevo o modificar la base de datos es un cambio de comportamiento/contrato de
base de datos, fuera del alcance de un refactor puramente estructural, y se documenta aquí en
lugar de improvisarse. Queda como **P1 para una fase futura dedicada de base de datos**: exponer
`usp_RegistrarEventoAuditoria` / `usp_ConsultarEventoAuditoriaPorCorrelationId` (o equivalente) y
migrar `AuditEventJdbcRepository` a usarlos vía `CanonicalStoredProcedureExecutor`, igual que el
resto de `persistence.sqlserver`.

## 8. Neutralidad de vendor de observabilidad

Prometheus, Grafana, Loki, Tempo y Alloy **nunca** tienen un Port ni un Adapter en Application ni
en `infrastructure.adapter`. Se integran exclusivamente vía estándares:

- **Métricas -> Prometheus**: Micrometer (`micrometer-registry-prometheus`) expone `/actuator/prometheus`;
  Prometheus hace scrape. Application no importa nada de Micrometer.
- **Tracing -> Tempo**: OpenTelemetry SDK exporta spans vía OTLP a un collector (Alloy), que los
  reenvía a Tempo. `infrastructure.observability.tracing.opentelemetry.TraceContextSnapshot` es
  el único punto de Infrastructure que toca el SDK de OTel.
- **Logs -> Loki**: logging estructurado (Logback) va a stdout; Alloy lo recolecta y lo envía a
  Loki. Ningún código productivo importa un cliente de Loki.
- **Dashboards -> Grafana**: consumidor puro de Prometheus/Loki/Tempo vía `infra/observability/grafana/provisioning`;
  nunca es tocado por Application ni por Infrastructure en tiempo de ejecución.

`InfrastructureStructureRulesTest` fija esta regla como invariante ArchUnit: ninguna clase de
`application..` puede depender de o nombrar `..prometheus..`, `..grafana..`, `..loki..`,
`..tempo..` o `..alloy..`.

**Para reemplazar Tempo por Jaeger** (o cualquier otro backend OTLP) sin tocar Application: se
cambia únicamente el exporter/endpoint OTLP en `application.yml` y la configuración de Alloy en
`infra/observability/alloy/`; cero cambios de código.

## 9. Composition Root

`infrastructure/config/adapters/**` es el único lugar donde se decide, en el arranque de Spring,
qué implementación concreta usar por capability, vía `app.adapters.<capability>.provider`
(`ConditionalOnProperty`, nunca `@Profile` — un profile mezcla entorno con tecnología). Los
adapters seleccionables (`*SqlServerAdapter`, `KeycloakIdentityProviderAdapter`,
`ReactorRealtimeAdapter`, `AuditEventJdbcRepository`) **no llevan** `@Component`/`@Service`/`@Repository`:
solo se instancian si el Composition Root los registra explícitamente
(`AdapterCompositionRootRulesTest` lo verifica). `infrastructure/config/wiring/**` es el
Composition Root de features: ensambla `UseCase` + `Interactor` a partir de Application Ports,
sin conocer ninguna tecnología (no importa `infrastructure.adapter.secondary..`).

### Cómo agregar un nuevo provider (ejemplos)

- **Agregar Postgres/otra base SQL**: crear `infrastructure/adapter/secondary/persistence/postgres/`
  con la misma forma que `persistence/sqlserver` (core/academic/reporting/authorization/support),
  un `config/adapters/persistence/postgres/Postgres*AdapterConfiguration` condicionado a
  `app.adapters.persistence.provider=postgres`, y una property `PostgresAdapterProperties`. Cero
  cambios en Application: los `*RepositoryPort` no cambian.
- **Agregar Auth0 (identidad)**: crear `infrastructure/adapter/secondary/identity/auth0/Auth0IdentityProviderAdapter`
  implementando `IdentityProviderPort`, más su `config/adapters/identity/auth0/Auth0IdentityAdapterConfiguration`
  condicionado a `app.adapters.identity.provider=auth0`. Nunca crear `Auth0Port`: el Port ya
  existe y es neutral.
- **Agregar otro proveedor realtime (o Redis Pub/Sub)**: crear
  `infrastructure/adapter/secondary/realtime/redis/RedisRealtimeAdapter` implementando
  `RealtimePublisherPort`, y `config/adapters/realtime/redis/RedisRealtimeAdapterConfiguration`
  condicionado a `app.adapters.realtime.provider=redis`. El primary adapter SSE
  (`primary/realtime/sse/**`) no cambia: solo cambia qué implementación llena
  `RealtimeStreamGateway`.
- **Agregar RabbitMQ**: es una capability nueva, no un provider de una capability existente. Solo
  se agrega un Application secondary port cuando exista un caso de uso real que lo necesite (ver
  §6); mientras no exista ese caso de uso, no se crea infraestructura especulativa.
- **Agregar MinIO (almacenamiento de archivos)**: mismo caso - requiere primero un
  `FileStoragePort` en Application cuando exista un caso de uso real (ver deuda pendiente,
  sección 10), luego `infrastructure/adapter/secondary/storage/minio/`.

## 10. KNOWN ARCHITECTURAL DEBT

Deuda identificada durante este refactor o ya documentada previamente, listada aquí para que
quede en un único lugar:

1. **`AuditEventJdbcRepository` hace DML directo** (INSERT/SELECT) en lugar de usar un stored
   procedure `usp_*` público. Ver sección 7. Relocalizado, no corregido (requiere cambio de base
   de datos, fuera de alcance de este refactor).
2. **`FileStoragePort` no existe todavía**: no hay capability de almacenamiento de archivos en
   Application. Cualquier adapter de MinIO/S3 debe esperar a que exista un caso de uso real que
   lo motive (regla de la sección 6): no se crea un Port especulativo.
3. **El `DataSource` no tiene todavía una property/Composition Root explícitamente
   provider-specific** más allá de `app.adapters.persistence.provider=sqlserver`: agregar un
   segundo motor SQL (Postgres) requerirá extraer `PersistenceAdapterProperties` en properties
   por provider si empiezan a divergir en forma (host/puerto/driver).
4. **RabbitMQ no existe**: no hay capability de mensajería asíncrona en Application. Se agrega
   siguiendo el mismo patrón capability->provider cuando exista un caso de uso real.
5. **Redis no existe**: ni como cache ni como Pub/Sub para realtime. Mismo criterio que RabbitMQ.
6. **Autorización contextual**: `InstitutionalScopePort`/`InstitutionalScopeSqlServerAdapter`
   cubre el caso actual (¿puede este usuario ver este grupo/sesión?); una autorización más fina
   basada en atributos (ABAC) no está implementada y no fue tocada por este refactor.
7. **Realtime en el frontend**: el backend expone `GET /api/v1/realtime/events` (SSE) y
   `POST /api/v1/realtime/emit` (utilidad de desarrollo); el consumo desde el frontend no es
   responsabilidad de este backend y no fue tocado.
8. **Naming inconsistente en un test ya existente**: `infrastructure/config/FeaturesBeansConfigTest.java`
   (no movido por este refactor porque prueba `SqlServerCoreRepositoryAdapterConfiguration`, un
   Composition Root de adapters, no una "Feature Config"/wiring) tiene un nombre de archivo que
   ya no refleja lo que prueba. Se deja documentado para una futura limpieza menor en lugar de
   generar churn adicional en este PR.

## 11. Ejemplos correctos e incorrectos

**Correcto**: `infrastructure.adapter.secondary.persistence.sqlserver.core.AsistenciaRepositorySqlServerAdapter`
implementa `application.secondaryports.repository.AsistenciaRepositoryPort`; se registra en
`config.adapters.persistence.sqlserver.SqlServerCoreRepositoryAdapterConfiguration` condicionado
a `app.adapters.persistence.provider=sqlserver`; no lleva `@Repository`.

**Incorrecto** (lo que este refactor eliminó): un paquete `infrastructure.adapter.secondary.repository.adapter`
que mezclaba el concepto "repository" (Port de Application) con el rol "adapter" (implementación),
sin indicar la tecnología (`sqlserver`) en la ruta. `InfrastructureStructureRulesTest.el_anidamiento_repository_adapter_no_reaparece`
impide que este anidamiento regrese.

**Correcto**: `JwtClaimsExtractor` es una interfaz (contrato interno de Infrastructure) que
`KeycloakJwtClaimsExtractor` implementa.

**Incorrecto** (lo que este refactor corrigió): `JwtClaimsAdapter` como nombre de interfaz -
"Adapter" implica implementación concreta, no contrato.

**Correcto**: una property de Micrometer/OTel en `application.yml` selecciona el exporter.

**Incorrecto**: una clase `PrometheusMetricsPort` en `application.secondaryports` - Prometheus es
un estándar de plataforma consumido vía Micrometer, no una capability de negocio.

---

Ver también `docs/architecture/infrastructure-refactor-mapping.md` para la tabla completa
ruta-anterior -> ruta-nueva con justificación de cada movimiento significativo.
