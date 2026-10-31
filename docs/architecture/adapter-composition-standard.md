---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-26
---

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
    identity:
      provider: ${APP_ADAPTERS_IDENTITY_PROVIDER:baseline}
    security:
      provider: ${APP_ADAPTERS_SECURITY_PROVIDER:baseline}
    storage:
      provider: ${APP_ADAPTERS_STORAGE_PROVIDER:local}
    realtime:
      provider: ${APP_ADAPTERS_REALTIME_PROVIDER:local-sse}
    audit:
      provider: ${APP_ADAPTERS_AUDIT_PROVIDER:logging}
    notification:
      provider: ${APP_ADAPTERS_NOTIFICATION_PROVIDER:log}
      composite:
        targets: ${APP_ADAPTERS_NOTIFICATION_COMPOSITE_TARGETS:log,outbox}
    vault:
      provider: ${APP_ADAPTERS_VAULT_PROVIDER:azure_keyvault}
    parameter-catalog:
      provider: ${APP_ADAPTERS_PARAMETER_CATALOG_PROVIDER:azure_appconfig}
    message-catalog:
      provider: ${APP_ADAPTERS_MESSAGE_CATALOG_PROVIDER:azure}
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

La regla anterior no aplica a los repositories JPA productivos definidos por [ADR-004](../adr/ADR-004-jpa-repository-architecture.md).
Esos componentes llevan obligatoriamente `@Repository`, implementan el secondary port directamente y pueden
declarar la condición del provider en la propia clase. No se registran mediante `@Bean` manual.

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

La distribución/durabilidad se evalúa en [LB-005](../baseline/LINEA_BASE.md). RabbitMQ fue una alternativa mencionada históricamente, no un provider implementado ni una obligación ratificada por LB-000. La selección futura requiere ADR y mantiene neutralidad. Detalle del provider actual: [reactive-realtime](reactive-realtime.md).

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

Contrato detallado: [keycloak-identity-provider](../security/keycloak-identity-provider.md). Estado de deuda: [TD-013..015](../baseline/TECHNICAL_DEBT.md#td-013).

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

**HISTÓRICO:** LB-002 introdujo selectores JDBC/JPA, adapter híbrido y fallback JDBC para certificar
el piloto de Asistencia. JPA-01 retiró esos mecanismos de la vertical y JPA-02A normalizó el
bootstrap; ya no describen el runtime vigente.

**TARGET ACTUAL:** [ADR-003](../adr/ADR-003-jpa-only-persistence.md) y LB-008 establecen
`PERSISTENCE_PROVIDER = JPA_ONLY`. No se crean nuevos selectores `jdbc|jpa`, adapters híbridos ni
fallback JDBC productivo. Una capacidad cerrada usa JPA en runtime; la coexistencia solo se tolera
durante su comparación de paridad.

Desde JPA-06A, [ADR-004](../adr/ADR-004-jpa-repository-architecture.md) fija la forma:

```text
Application Port -> @Repository XxxJpaRepository -> EntityManager
```

No hay `SqlServerAdapter` ni `JpaCommandPersistence/JpaQueryPersistence` delegadores entre el port y el
repository. El component scanning estándar reemplaza los beans manuales de repositorio; la configuración
técnica de Hibernate/JPA se conserva.

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

## 13. Notification — estado actual (NOTIF-001 / NOTIF-002A)

AsistenciasUCO desacopla el despacho de notificaciones académicas mediante el puerto secundario `NotificacionPort` (`application/secondaryports/notification`).

La capacidad soporta tres Composition Roots mutuamente excluyentes vía `app.adapters.notification.provider`:

1. `log` (`LogNotificacionAdapterConfiguration`): adaptador síncrono que registra las notificaciones en el logger institucional sin mutación de BD (modo predeterminado).
2. `outbox` (`OutboxNotificacionAdapterConfiguration`): adaptador transaccional duradero que persiste en `dbo.NotificacionSaliente` mediante `usp_registrar_notificacion_saliente`, con un despachador en segundo plano (`NotificacionOutboxDispatcher`) que verifica disponibilidad del canal (`sender.disponible()`), reserva lotes vía `usp_reservar_notificaciones_salientes`, entrega al canal SPI `NotificacionChannelSender`, reporta éxito o fallo reintentable/definitivo y ejecuta depuración periódica de filas terminales.
3. `composite` (`CompositeNotificacionAdapterConfiguration`): adaptador compuesto que realiza *fan-out* a múltiples destinos hojas configurados en `app.adapters.notification.composite.targets` (p. ej. `log,outbox`), garantizando aislamiento contra fallos parciales.

### 13.1 Selección de canal del outbox (`sender=log|smtp`) y módulo `notification.*` (NOTIF-002A)

Sobre el proveedor `outbox`, el canal concreto de salida (`NotificacionChannelSender`) se selecciona mediante `app.providers.outbox-notification.sender` ([ADR-006](../adr/ADR-006-notification-email-and-templates.md)):

- `log` (predeterminado): `LogNotificacionChannelSender` registra el envío en el log estructurado sin tráfico de red externo.
- `smtp` (`SmtpNotificacionAdapterConfiguration`): activa el módulo removible `infrastructure.adapter.secondary.notification.{template,contact,channel.smtp}` compuesto por:
  - **Plantillas editables (`template`):** consulta la plantilla activa versionada en SQL Server (`usp_consultar_plantilla_notificacion_activa`) con caché por `(tipo, idioma)` (`PlantillaNotificacionJpaRepository`), desciende al catálogo de respaldo del classpath (`PlantillaPorDefectoCatalogo`) o al texto almacenado en la fila si faltan marcadores obligatorios, y renderiza texto plano + HTML escapado envuelto en un layout institucional fijo (`PlantillaRenderer`).
  - **Resolución de contacto al entregar (`contact`):** resuelve correos activos y habilitados en tiempo de entrega vía `usp_consultar_contacto_notificacion` (`ContactoDestinatarioJpaRepository`), manteniendo la minimización de PII en el outbox.
  - **Canal SMTP y tope de envíos (`channel.smtp`):** `SmtpNotificacionChannelSender` agrupa destinatarios en bloques BCC (`max-bcc`), consume cupo de `TopeEnvios` (ventanas por minuto y por día; cuando se agota, `disponible()=false` evita reservar filas o gastar intentos) y envía mediante `CorreoTransport` (`JakartaMailCorreoTransport`).

### 13.2 Modo de pruebas con redirección (D15)

Para realizar pruebas con servidor SMTP sin riesgo de enviar correos a usuarios reales, `SmtpNotificationProperties` incluye `app.providers.smtp-notification.test-mode`:

- `enabled` (default `false`): debe permanecer en `false` en producción. Al iniciar con `enabled=true`, se emite un único `WARN` de arranque sin exponer las direcciones.
- `redirect-to` (1..5 direcciones válidas separadas por coma, obligatorias si `enabled=true` con validación fail-fast): cuando el modo de pruebas está activo, los contactos reales se validan igual que en producción (si no hay destinatarios válidos se lanza `NotificacionNoEntregableException` y no se envía nada), pero el BCC se reemplaza exclusivamente por `redirect-to`, el asunto se prefija con `[PRUEBA] `, el cuerpo (texto plano y HTML) inicia con el párrafo de aviso indicando cuántos destinatarios originales se omitieron (`N`) sin incluir correos ni nombres reales, y se incrementa la métrica `notification.email.redirected.total`.

Las properties tipadas residen en `NotificationAdapterProperties`, `OutboxNotificationProperties` y `SmtpNotificationProperties`. Ninguna clase de Application o Domain conoce el provider ni el canal activo (`NotificationEmailModuleIsolationRulesTest` AR-07..AR-10).

---

## 14. Source/package consistency

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

## 15. Matriz actual de capabilities

| Capability | Port / SPI | Adapter actual | Selector | Estado | Trabajo pendiente |
|---|---|---|---|---|---|
| Persistence | `*RepositoryPort`, `*QueryPort`, `*CommandPort`, `InstitutionalScopePort` | SQL Server adapters: Asistencia JPA-only; resto de verticales JDBC heredado | Ninguno en Asistencia; el resto no tiene selector | JPA-02A PASS: bootstrap estándar de Boot y `JpaTransactionManager`; Asistencia cumple ADR-003; resto AS-IS JDBC | JPA-02B READY_WITH_SCOPED_DB_BLOCKER para commands de Sesión + Grupo; retirar el JDBC restante por capacidad; DataSource provider-specific antes de otra DB |
| Secret Vault | `SecretVaultPort` | `AzureKeyVaultAdapter`, `LocalEnvSecretVaultAdapter` | `app.adapters.vault.provider=azure_keyvault` | Reemplazable; Azure usa `DefaultAzureCredential`; Caffeine local 50/5 min | Evidencia operacional MV-003; seguridad webhook en LB-001D.2 |
| Parameter Catalog | `ParameterCatalogPort` | `AzureAppConfigParameterCatalogAdapter`, `SqlServerParameterCatalogAdapter` | `app.adapters.parameter-catalog.provider=azure_appconfig` | Reemplazable; Azure usa Caffeine local 1000/10 min | Evidencia operacional MV-003 |
| Message Catalog | `MessageCatalogPort` | `AzureAppConfigMessageCatalogAdapter`, `SqlServerMessageCatalogAdapter` | `app.adapters.message-catalog.provider=azure` | Reemplazable; Azure usa dos caches locales 2000/30 min; label `es` para usuario | Evidencia operacional MV-003 |
| Catalog/Vault invalidation | `CatalogInvalidationPort` | `CompositeCatalogInvalidationAdapter` | input Event Grid | Invalida parámetros, mensajes, secretos o todo; Event Grid entra por InputPort | Decisión Azure→realtime DR-AZ-001; aislamiento cloud test LB-001D.2 |
| Identity Provisioning | `IdentityProviderPort` | `KeycloakIdentityProviderAdapter` | `app.adapters.identity.provider=keycloak` | Reemplazable por provider; solo Keycloak | E2E y completar integración de roles/flujos institucionales pendientes |
| Runtime Security | `JwtClaimsExtractor` | `KeycloakJwtClaimsExtractor` | `app.adapters.security.provider=keycloak` | Reemplazable por SPI; solo Keycloak | E2E y authorization hardening contextual |
| Password Encoding | `PasswordEncoderPort` | Spring password adapter | configuración existente | Desacoplado | Sin deuda de provider relevante en esta fase |
| Storage | `FileStoragePort` pendiente | filesystem dentro de `ArchivoController` | `app.adapters.storage.provider=local` preparado | Todavía no reemplazable | Extraer InputPort/UseCase/Port antes de MinIO |
| Realtime | `RealtimePublisherPort` | `ReactorRealtimeAdapter` + SSE gateway | `app.adapters.realtime.provider=local-sse` | Provider seleccionado por Composition Root; solo `local-sse` | Angular SSE autenticado, API First, distribución/durabilidad con proveedor por decidir en ADR |
| Audit | `AuditEventPublisher` | logging + soporte SQL existente | `app.adapters.audit.provider=logging` | Parcialmente desacoplado | Durable provider si se requiere |
| Observability | APIs OTel/Micrometer/logging | OTel + Micrometer + logs estructurados | configuración externa | Provider-neutral | Validación operacional continua |
| Notification | `NotificacionPort` / `NotificacionChannelSender` | `LogNotificacionAdapter`, `OutboxNotificacionAdapter` (`LogNotificacionChannelSender`, `SmtpNotificacionChannelSender`), `CompositeNotificacionAdapter` | `app.adapters.notification.provider=log` (`log` \| `outbox` \| `composite`) y `app.providers.outbox-notification.sender=log` (`log` \| `smtp`) | Desacoplado (NOTIF-001 / NOTIF-002A); outbox durable con plantillas versionadas en BD, resolución de contacto al entregar, canal SMTP con tope de envíos y modo de pruebas con redirección (`test-mode`) | Administración de plantillas (NOTIF-002B), credenciales SMTP definitivas de TI y otros canales (SMS, push) |

---

## 16. Seguimiento de deuda

Única autoridad de estado/prioridad: [TECHNICAL_DEBT](../baseline/TECHNICAL_DEBT.md). Identity: TD-013..015; Storage/ownership: TD-004; realtime/frontend/distribución/contrato: TD-002,003,017,018; DataSource: TD-012; autorización contextual: TD-016; Notification: TD-066. Las limitaciones técnicas descritas arriba no son un segundo ledger.

---

## 17. Checklist para un nuevo provider

Antes de considerar integrado un provider nuevo (cumplido para Notification NOTIF-001):

```text
[x] existe un Port/SPI neutral (NotificacionPort / NotificacionChannelSender)
[x] el adapter concreto vive en Infrastructure
[x] el adapter no se autoregistra si la capability es seleccionable
[x] existe Composition Root con @ConditionalOnProperty
[x] existen properties tipadas cuando aplican (NotificationAdapterProperties, OutboxNotificationProperties)
[x] Application no conoce el provider
[x] no se usa @Profile para seleccionar tecnología
[x] no hay Service Locator
[x] existen tests ApplicationContextRunner del wiring (NotificationWiringTest WI-01..WI-12)
[x] ArchUnit sigue verde (MessagingIsolationArchitectureTest AR-01..AR-05)
[x] JaCoCo global sigue por encima del gate (Líneas 97.2%, Ramas 87.8%)
[x] Sonar New Code pasa
[x] CodeQL/Dependency Review pasan
[x] documentación y matriz de capabilities están actualizadas
```

Este documento representa el estado vigente después de la Fase 3.1 de realtime y debe
actualizarse cada vez que una capability pasa de "preparada" a "implementada".

Las caches Caffeine anteriores son por JVM/proceso. No ofrecen coherencia distribuida; Event Grid reduce la ventana de datos stale mediante invalidación push, sin convertirlas en cache compartida. Véase [Azure Runtime Integration](../integration/azure-runtime-integration.md).
