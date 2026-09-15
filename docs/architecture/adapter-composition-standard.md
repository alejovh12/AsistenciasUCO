# Estándar de Composition Root y selección de adapters

## 1. Propósito

AsistenciasUCO usa Clean Architecture / Ports & Adapters. Domain y Application expresan
**capacidades** mediante contratos neutrales; Infrastructure decide **qué tecnología** satisface
cada capacidad.

La selección tecnológica ocurre una sola vez al construir el contexto de Spring:

```text
Domain
  ↑
Application
  |
  +--> Secondary Port
           |
           v
      Composition Root
           |
    +------+------+
    |             |
 Adapter A     Adapter B
```

Regla principal:

```text
PROFILE  = entorno
PROVIDER = tecnología
```

Los perfiles (`local`, `test`, `staging`, `prod`) no seleccionan tecnologías. Los providers sí:

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

---

## 2. Reglas obligatorias

### 2.1 Dirección de dependencias

```text
Primary Adapter -> InputPort
Application UseCase -> SecondaryPort
Secondary Adapter -> SecondaryPort
Composition Root -> adapters concretos
```

Nunca:

```text
Application -> Infrastructure
Application -> Keycloak
Application -> Reactor
Application -> RabbitMQ
Application -> Redis
Application -> MinIO
```

### 2.2 Ports neutrales

Un Port representa una capacidad del sistema, no una herramienta.

Correcto:

```text
IdentityProviderPort
RealtimePublisherPort
FileStoragePort
EventPublisherPort
```

Incorrecto:

```text
KeycloakPort
ReactorPort
RabbitMqPort
MinioPort
```

### 2.3 El adapter concreto no se autoregistra

Cuando una capability tiene provider seleccionable, el adapter tecnológico no debe usar:

```java
@Component
@Service
@Repository
```

para decidir por sí mismo que debe existir.

El registro pertenece al Composition Root:

```java
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = "app.adapters.realtime",
    name = "provider",
    havingValue = "local-sse"
)
class LocalSseRealtimeAdapterConfiguration {
    // @Bean ...
}
```

Así cambiar un provider no exige tocar Domain, Application ni Controllers.

### 2.4 No Service Locator

Prohibido:

```java
Map<String, IdentityProviderPort> providers;
AdapterFactory.get(provider);
applicationContext.getBean(provider);
```

También está prohibido leer `Environment`, `@ConfigurationProperties` o hacer `switch(provider)`
desde un UseCase.

---

## 3. `app.adapters.*` vs `app.providers.*`

`app.adapters.*` selecciona **qué implementación** se usa.

```yaml
app:
  adapters:
    realtime:
      provider: local-sse
```

`app.providers.*` contiene **cómo se configura** una tecnología ya seleccionada.

```yaml
app:
  providers:
    keycloak-identity:
      server-url: ...
      realm: ...
      admin-client-id: ...
    keycloak-security:
      issuer-uri: ...
      expected-audience: ...
    local-storage:
      upload-directory: ...
```

No deben mezclarse.

---

## 4. Properties tipadas

Las selectors están representadas mediante `@ConfigurationProperties` bajo:

```text
infrastructure.config.properties.adapters
```

Por ejemplo:

```text
RealtimeAdapterProperties.Provider.LOCAL_SSE
IdentityAdapterProperties.Provider.KEYCLOAK
```

Las properties específicas de tecnología viven bajo:

```text
infrastructure.config.properties.providers
```

El objetivo es fallar temprano ante configuración inválida y evitar strings tecnológicos
dispersos por Application.

---

## 5. Patrón de Composition Root

Estructura esperada:

```text
infrastructure/config/adapters/
├── identity/
│   └── keycloak/
├── persistence/
│   └── sqlserver/
├── security/
│   └── keycloak/
└── realtime/
    └── localsse/
```

Cada provider implementado debe tener:

1. adapter concreto;
2. properties específicas si las necesita;
3. configuración `@ConditionalOnProperty`;
4. tests de wiring con `ApplicationContextRunner`;
5. documentación de reemplazo y limitaciones.

Ejemplo ya existente:

```text
IdentityProviderPort
  -> KeycloakIdentityProviderAdapter
  -> KeycloakIdentityAdapterConfiguration
```

Ejemplo realtime desde Fase 3.1:

```text
RealtimePublisherPort
  -> ReactorRealtimeAdapter
  -> LocalSseRealtimeAdapterConfiguration
```

---

## 6. Fail-fast

Para providers actuales se mantiene `matchIfMissing = true` cuando existe un default explícito en
`application.yml`, por compatibilidad con ambientes existentes.

Si se configura un provider distinto sin Composition Root compatible, el sistema debe fallar al
crear las dependencias requeridas; no debe existir fallback silencioso a otra tecnología.

Antes de incorporar providers alternativos en producción debe evaluarse eliminar gradualmente
`matchIfMissing` cuando todos los ambientes declaren explícitamente su provider.

---

## 7. Realtime — estado actual

### 7.1 Contrato

Application define:

```text
application.secondaryports.realtime.RealtimePublisherPort
application.secondaryports.realtime.RealtimeEvent
```

El puerto no expone Reactor ni SSE.

### 7.2 Provider implementado

```text
app.adapters.realtime.provider=local-sse
```

Composition Root:

```text
infrastructure.config.adapters.realtime.localsse
└── LocalSseRealtimeAdapterConfiguration
```

Tecnología:

```text
Project Reactor
Sinks.Many<RealtimeEvent>
Spring MVC SSE
```

Adapter:

```text
ReactorRealtimeAdapter
```

El adapter no se autoregistra; el Composition Root crea la única instancia usada tanto por
Application (`RealtimePublisherPort`) como por la salida SSE local.

### 7.3 Capa HTTP

El controller no depende del adapter secundario concreto. Usa:

```text
RealtimeEventsController
  -> RealtimeStreamGateway
```

El gateway es una interfaz interna de Infrastructure para adaptar la salida reactiva al contrato
HTTP. El Composition Root registra su implementación para el provider local-sse.

### 7.4 Semántica

El provider `local-sse` es:

- local a una JVM;
- best-effort;
- sin durabilidad;
- sin replay;
- sin entrega garantizada a clientes desconectados;
- orientado a fan-out de eventos a conexiones SSE activas.

La fuente de verdad continúa siendo la API/DB, no SSE.

### 7.5 Evolución prevista

Hoy:

```text
UseCase
  -> RealtimePublisherPort
  -> ReactorRealtimeAdapter
  -> SSE
```

Futuro distribuido:

```text
UseCase
  -> EventPublisherPort
  -> RabbitMQ
  -> Consumer
  -> RealtimePublisherPort
  -> ReactorRealtimeAdapter
  -> SSE
```

RabbitMQ resolverá distribución/durabilidad; Reactor seguirá resolviendo fan-out local.

Más detalle:

```text
docs/architecture/reactive-realtime.md
```

---

## 8. Identity Provisioning

Capability:

```text
IdentityProviderPort
```

Provider actual:

```text
app.adapters.identity.provider=keycloak
```

Composition Root:

```text
KeycloakIdentityAdapterConfiguration
```

Adapter:

```text
KeycloakIdentityProviderAdapter
```

Identity provisioning administrativo y Runtime Security son capabilities separadas aunque hoy
usen el mismo Keycloak.

Deudas actuales se mantienen documentadas en:

```text
docs/security/keycloak-identity-provider.md
```

---

## 9. Runtime Security

Runtime Security usa:

```text
JwtClaimsExtractor
JwtDecoder
InstitutionalJwtAuthenticationConverter
```

Provider:

```text
app.adapters.security.provider=keycloak
```

El adapter Keycloak interpreta claims; `SecurityConfig` mantiene reglas HTTP neutrales.

No se deben introducir ports por vendor en Application.

Ver:

```text
docs/security/runtime-security-provider-architecture.md
```

---

## 10. Persistence

SQL Server es el único provider implementado actualmente.

Application depende de:

```text
*RepositoryPort
*QueryPort
*CommandPort
InstitutionalScopePort
```

Adapters SQL viven bajo Infrastructure.

Antes de incorporar una segunda DB debe hacerse provider-specific también el ownership del
`DataSource`, de modo que un provider no JDBC no fuerce la creación de un datasource SQL Server.

No se modifica la regla contractual actual:

- Java productivo invoca procedimientos públicos `usp_*`;
- no invoca `usp_*_interno`;
- lecturas respetan las views/contratos definidos;
- la DB no se modifica desde esta fase de infraestructura.

---

## 11. Storage

Estado actual:

```text
ArchivoController -> filesystem local
```

Todavía falta extraer:

```text
GuardarArchivoInputPort
GuardarArchivoUseCase
FileStoragePort
LocalFileStorageAdapter
```

Antes de MinIO el flujo objetivo es:

```text
ArchivoController
  -> GuardarArchivoInputPort
  -> UseCase
  -> FileStoragePort
  -> LocalFileStorageAdapter / MinioFileStorageAdapter
```

El controller nunca debe depender directamente de `FileStoragePort`.

---

## 12. Audit y observabilidad

Observabilidad utiliza estándares:

```text
OpenTelemetry
Micrometer
logging estructurado
OTLP
```

No se crean ports como:

```text
GrafanaPort
TempoPort
LokiPort
PrometheusPort
```

porque esos backends se seleccionan externamente mediante collectors/configuración.

Realtime usa el mismo contexto de observabilidad sin introducir OpenTelemetry dentro de
Application.

---

## 13. Source/package consistency

La ruta física debe coincidir con el package Java.

Ejemplo:

```java
package co.edu.uco.asistenciasuco.application.features.coordinador.common.dto;
```

debe vivir en:

```text
src/main/java/co/edu/uco/asistenciasuco/application/features/coordinador/common/dto/
```

`SourcePackageConsistencyTest` protege esta regla.

---

## 14. Matriz actual de capabilities

| Capability | Port / SPI | Adapter actual | Selector | Estado | Trabajo pendiente |
|---|---|---|---|---|---|
| Persistence | `*RepositoryPort`, `*QueryPort`, `*CommandPort`, `InstitutionalScopePort` | SQL Server adapters | `app.adapters.persistence.provider=sqlserver` | Ports desacoplados; solo SQL Server | DataSource provider-specific antes de otra DB |
| Identity Provisioning | `IdentityProviderPort` | `KeycloakIdentityProviderAdapter` | `app.adapters.identity.provider=keycloak` | Reemplazable por provider; solo Keycloak | E2E y completar integración de roles/flujos institucionales pendientes |
| Runtime Security | `JwtClaimsExtractor` | `KeycloakJwtClaimsExtractor` | `app.adapters.security.provider=keycloak` | Reemplazable por SPI; solo Keycloak | E2E y authorization hardening contextual |
| Password Encoding | `PasswordEncoderPort` | Spring password adapter | configuración existente | Desacoplado | Sin deuda de provider relevante en esta fase |
| Storage | `FileStoragePort` pendiente | filesystem dentro de `ArchivoController` | `app.adapters.storage.provider=local` preparado | Todavía no reemplazable | Extraer InputPort/UseCase/Port antes de MinIO |
| Realtime | `RealtimePublisherPort` | `ReactorRealtimeAdapter` + SSE gateway | `app.adapters.realtime.provider=local-sse` | Provider seleccionado por Composition Root; solo `local-sse` | Angular SSE autenticado, API First, distribución/durabilidad con RabbitMQ |
| Audit | `AuditEventPublisher` | logging + soporte SQL existente | `app.adapters.audit.provider=logging` | Parcialmente desacoplado | Durable provider si se requiere |
| Observability | APIs OTel/Micrometer/logging | OTel + Micrometer + logs estructurados | configuración externa | Provider-neutral | Validación operacional continua |

---

## 15. Deuda conocida vigente

1. **Identity:** completar validación E2E y flujos de provisioning institucional pendientes.
2. **Storage:** introducir `GuardarArchivoInputPort`/`FileStoragePort` y ownership/autorización
   contextual antes de MinIO.
3. **Realtime frontend:** Angular aún debe consumir SSE con `Authorization: Bearer`, reconexión y
   refresh de token.
4. **Realtime distribuido:** `local-sse` vive en una sola JVM; RabbitMQ será necesario para
   distribución entre instancias y eventos durables.
5. **Realtime contract:** formalizar SSE/eventos en la fase API First / Contract First.
6. **Persistence:** hacer `DataSource` provider-specific antes de una segunda DB.
7. **Authorization contextual:** completar Layer 2 en endpoints documentados en
   `runtime-security-provider-architecture.md`.
8. **Storage security:** `/api/v1/archivos/**` mantiene deuda de ownership/contexto hasta la fase
   Storage.
9. **Endpoint diagnóstico realtime:** decidir antes de producción si `/api/v1/realtime/emit`
   permanece, se condiciona por property o se elimina.

---

## 16. Checklist para un nuevo provider

Antes de considerar integrado un provider nuevo:

```text
[ ] existe un Port/SPI neutral
[ ] el adapter concreto vive en Infrastructure
[ ] el adapter no se autoregistra si la capability es seleccionable
[ ] existe Composition Root con @ConditionalOnProperty
[ ] existen properties tipadas cuando aplican
[ ] Application no conoce el provider
[ ] no se usa @Profile para seleccionar tecnología
[ ] no hay Service Locator
[ ] existen tests ApplicationContextRunner del wiring
[ ] ArchUnit sigue verde
[ ] JaCoCo global sigue por encima del gate
[ ] Sonar New Code pasa
[ ] CodeQL/Dependency Review pasan
[ ] documentación y matriz de capabilities están actualizadas
```

Este documento representa el estado vigente después de la Fase 3.1 de realtime y debe
actualizarse cada vez que una capability pasa de "preparada" a "implementada".
