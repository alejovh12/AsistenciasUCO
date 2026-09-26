---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

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
  piezas se configuran vía `application.yml`/`pom.xml`, nunca vía un Port de
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

**Excepción AS-IS:** `AuditEventJdbcRepository` todavía hace DML directo. La descripción, impacto, prioridad y condición de cierre se conservan exclusivamente en [TD-010](../baseline/TECHNICAL_DEBT.md#td-010). No es precedente para nuevos adapters ni autorización para crear SP en esta tarea.

## 8. Neutralidad de vendor de observabilidad

Prometheus, Grafana, Loki, Tempo y Alloy **nunca** tienen un Port ni un Adapter en Application ni
en `infrastructure.adapter`. Se integran exclusivamente vía estándares:

- **Métricas -> Prometheus**: Micrometer (`micrometer-registry-prometheus`) expone `/actuator/prometheus`;
  Prometheus hace scrape. Application no importa nada de Micrometer.
- **Tracing -> Tempo**: OpenTelemetry SDK exporta spans vía OTLP a un collector (Alloy), que los
  reenvía a Tempo. `infrastructure.observability.tracing.opentelemetry.TraceContextSnapshot` es
  el único punto de Infrastructure que toca el SDK de OTel.
- **Logs -> Loki**: logging estructurado (Logback) se escribe actualmente en archivos JSONL; Alloy
  lee el directorio montado y lo envía a Loki (`application.yml` y `infra/observability/alloy/config.alloy`). stdout/OTLP en cloud es evolución, no el flujo local actual. Ningún código productivo importa un cliente de Loki.
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

## 10. Seguimiento de deuda

Consultar el [ledger único](../baseline/TECHNICAL_DEBT.md): auditoría DML TD-010; storage TD-004; DataSource TD-012; autorización TD-016; frontend/realtime TD-017; nombre de test TD-026. RabbitMQ/Redis son evoluciones sin implementación, sujetas a necesidad real y [LINEA_BASE](../baseline/LINEA_BASE.md), no requisitos de crear infraestructura por anticipado.

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

Ver también `docs/archive/infrastructure-refactor-mapping.md` para la tabla completa
ruta-anterior -> ruta-nueva con justificación de cada movimiento significativo.
