---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

# Reactividad realtime — Fase 3.1

## 1. Estado y objetivo

Esta fase introduce una vertical realtime real para AsistenciasUCO sin convertir artificialmente
el backend completo a un stack reactivo.

Estado de la decisión:

- **Backend principal:** Spring MVC + JDBC bloqueante + SQL Server.
- **Realtime local:** Project Reactor + Server-Sent Events (SSE).
- **Selección del provider:** Composition Root con
  `app.adapters.realtime.provider=local-sse`.
- **Seguridad:** OAuth2 Resource Server / JWT Bearer; el stream no es público.
- **Durabilidad:** ninguna en esta fase. El canal local es **best-effort y efímero**.
- **Distribución entre instancias:** no existe todavía; se evalúa en LB-005 con ADR de proveedor.
- **Application:** no depende de Reactor, WebFlux, SSE ni OpenTelemetry.

La finalidad de Reactor aquí es modelar correctamente un flujo push de múltiples eventos y
múltiples consumidores. No se usa para maquillar como reactivo el acceso JDBC.

---

## 2. Decisiones que NO se tomaron

### 2.1 No se migró a WebFlux

No se agregó `spring-boot-starter-webflux`. Spring MVC soporta tipos reactivos de retorno,
incluido `Flux<ServerSentEvent<T>>`, por medio de `ReactiveAdapterRegistry`.

Esto no convierte el stack servlet en I/O no bloqueante. En Spring MVC las escrituras al
`HttpServletResponse` continúan siendo bloqueantes y el framework las ejecuta mediante su
`AsyncTaskExecutor`.

En este proyecto:

```yaml
spring:
  main:
    web-application-type: servlet
    keep-alive: true
  threads:
    virtual:
      enabled: true
```

Por lo tanto Spring Boot suministra el executor asíncrono de MVC usando virtual threads. La
decisión de esta fase es deliberada: conservar MVC/JDBC y usar una fuente reactiva para el
stream, sin introducir un segundo modelo de servidor solo para SSE.

### 2.2 No se hizo JDBC falsamente reactivo

Está prohibido introducir patrones como:

```java
Mono.fromCallable(() -> jdbcTemplate.query(...));
```

La llamada JDBC sigue bloqueando aunque esté envuelta en un `Mono`. Los repositorios y stored
procedures existentes permanecen sin cambios. Una futura migración real del acceso a datos
requeriría un driver/protocolo no bloqueante y una decisión arquitectónica separada; no forma
parte de esta fase.

### 2.3 No se debilitó la seguridad para SSE

No se permite:

```text
/api/v1/realtime/stream?token=<jwt>
```

ni hacer público `/api/v1/realtime/**`.

El JWT continúa viajando mediante:

```http
Authorization: Bearer <token>
```

---

## 3. Arquitectura

```text
                           APPLICATION
                               |
                    RealtimePublisherPort
                               |
                               v
                    +----------------------+
                    |  Composition Root    |
                    | provider=local-sse   |
                    +----------+-----------+
                               |
                               v
                    ReactorRealtimeAdapter
                      directBestEffort()
                               |
                            Flux<Event>
                               |
                               v
                    RealtimeStreamGateway
                               |
                               v
                    RealtimeEventsController
                               |
                     Server-Sent Events
                               |
                               v
                         Angular / cliente
```

El flujo de negocio conectado al Golden Path es `RegistrarAsistenciasSesionUseCaseImpl → AsistenciaRepositoryPort.registrarAsistenciasSesion → RealtimePublisherPort.publish(ASISTENCIAS_SESION_ACTUALIZADAS)`, solo tras el retorno exitoso de persistencia. Véanse [Golden Path](../baseline/GOLDEN_PATH_ASISTENCIA.md) y [contrato de eventos](../contracts/REALTIME_EVENT_STANDARD.md).

Realtime es secundario. No existe una transacción distribuida SQL ↔ SSE.

---

## 4. Application: contrato neutral

### 4.1 `RealtimePublisherPort`

Ubicación:

```text
application.secondaryports.realtime.RealtimePublisherPort
```

Contrato:

```java
public interface RealtimePublisherPort {
    void publish(RealtimeEvent event);
}
```

El puerto expresa una capability, no una tecnología. No contiene `Flux`, `Mono`,
`ServerSentEvent`, `Sinks`, `WebSocket` ni tipos de broker.

El contrato es **best-effort**: una falla del mecanismo realtime no puede convertir en fallida
una operación de negocio que ya fue persistida. El adapter debe manejar internamente rechazo,
ausencia de consumidores o contención de emisión.

### 4.2 `RealtimeEvent`

El evento de Application usa únicamente tipos Java:

- `UUID eventId`
- `String type`
- `Instant occurredAt`
- `String correlationId`
- `String traceId`
- `String spanId`
- `Map<String, Object> payload`

`RealtimeEvent.of(type, payload)` crea identidad y tiempo, dejando el contexto de observabilidad
sin resolver. El adapter de infraestructura lo completa al publicar.

El `payload` debe contener exclusivamente datos de negocio necesarios para refrescar el cliente.
No debe contener:

- access tokens;
- refresh tokens;
- contraseñas;
- secretos;
- cookies;
- datos de sesión de seguridad.

El constructor defensivo usa `Map.copyOf(...)` para impedir la modificación directa del mapa
después de crear el evento.

---

## 5. Composition Root y provider

El proyecto ya define:

```yaml
app:
  adapters:
    realtime:
      provider: ${APP_ADAPTERS_REALTIME_PROVIDER:local-sse}
```

y la propiedad tipada:

```text
RealtimeAdapterProperties.Provider.LOCAL_SSE
```

El provider local se registra exclusivamente desde:

```text
infrastructure.config.adapters.realtime.localsse
└── LocalSseRealtimeAdapterConfiguration
```

La configuración está condicionada por:

```java
@ConditionalOnProperty(
    prefix = "app.adapters.realtime",
    name = "provider",
    havingValue = "local-sse",
    matchIfMissing = true
)
```

`ReactorRealtimeAdapter` y `LocalSseRealtimeStreamGateway` **no se autoregistran** con
`@Component`, `@Service` ni `@Repository`.

Esto mantiene el mismo estándar usado por persistencia, identidad y seguridad:

```text
PROFILE  = entorno
PROVIDER = tecnología
```

Cambiar el provider nunca debe introducir `if`, `switch`, `Environment` o service locator dentro
de Application.

---

## 6. Política del `Sinks.Many`

El adapter local usa:

```java
Sinks.many().multicast().directBestEffort()
```

La elección es intencional.

### 6.1 Sin consumidores

Si no existe ningún subscriber activo, `tryEmitNext(...)` falla inmediatamente con un
`EmitResult` no exitoso. El evento **no se almacena** para un cliente futuro.

Esto evita entregar información obsoleta a un usuario que se conecta después y hace explícita la
semántica de esta fase: realtime local no es una cola durable.

### 6.2 Varios consumidores

Los clientes con demanda reciben el mismo evento.

### 6.3 Consumidor lento

`directBestEffort` permite que un consumidor preparado siga recibiendo aunque otro no tenga
demanda. El consumidor lento puede perder ese elemento sin frenar a los demás.

La API del sink no informa al publisher cada pérdida individual de un subscriber cuando al menos
otro consumidor pudo aceptar el elemento. Por eso la métrica `realtime.events.dropped` representa
**rechazos globales del intento de emisión**, no una métrica exacta de pérdidas por cliente.

### 6.4 Emisión concurrente

Los sinks seguros detectan acceso concurrente. `tryEmitNext` puede devolver
`FAIL_NON_SERIALIZED`. La política actual es:

```text
log WARN
+ incrementar métrica de rechazo
+ NO relanzar al caso de uso
```

Si en una fase futura el volumen de publishers concurrentes exige serialización/reintento, debe
implementarse como una decisión explícita y medirse; no se añade un retry infinito en esta fase.

### 6.5 Sin replay

No existe:

- replay histórico;
- `Last-Event-ID`;
- almacenamiento local de eventos;
- garantía "at least once";
- garantía de entrega a un cliente desconectado.

Para esas necesidades se requiere mensajería durable y/o recuperación del estado mediante API.

---

## 7. Observabilidad

El adapter enriquece el evento justo antes de emitir usando la infraestructura existente:

```text
CorrelationIdContext
TraceContextSnapshot
```

Application no importa OpenTelemetry.

Métricas lógicas Micrometer:

```text
realtime.events.published
realtime.events.dropped
realtime.subscribers.active
```

En Prometheus se normalizan al formato correspondiente, por ejemplo los counters terminan
expuestos con sufijo `_total`.

Semántica:

- `published`: el sink aceptó el elemento para al menos un consumidor.
- `dropped`: el intento completo fue rechazado, por ejemplo sin subscribers o por un
  `EmitResult` no exitoso.
- `subscribers.active`: subscriptions activas sobre el stream local.

No se usan `userId`, `correlationId`, `traceId`, `eventId` ni otros identificadores de alta
cardinalidad como labels de Prometheus.

Los IDs sí pueden aparecer en logs estructurados para reconstrucción de incidentes.

---

## 8. Capa HTTP y SSE

Controller:

```text
GET  /api/v1/realtime/stream
GET  /api/v1/realtime/status
POST /api/v1/realtime/emit
```

### 8.1 `/stream`

Requiere `grupoId` UUID y Bearer. `LocalSseRealtimeStreamGateway.subscribe` comprueba titularidad docente una vez al suscribirse y filtra eventos por `payload.grupo`; los eventos sin grupo no se entregan por este canal. La autenticación HTTP por sí sola no concede la suscripción.

Produce:

```http
Content-Type: text/event-stream
```

Los eventos de negocio se envían como:

```text
id: <eventId>
event: <type>
data: <RealtimeEventResponse JSON>
```

`RealtimeEventResponse` expone:

- `eventId`
- `type`
- `occurredAt`
- `correlationId`
- `payload`

`traceId` y `spanId` permanecen del lado servidor; no se exponen al navegador como parte del
contrato actual.

### 8.2 Heartbeat

Además de eventos de negocio, cada conexión genera un comentario SSE cada 25 segundos:

```text
:heartbeat
```

El heartbeat:

- evita que una conexión ociosa parezca completamente inactiva para proxies/intermediarios;
- no es un evento de negocio;
- no pasa por `RealtimePublisherPort`;
- no incrementa las métricas de eventos publicados;
- termina automáticamente cuando el cliente cancela la suscripción.

El valor de 25 segundos conserva la intención del antiguo `RealtimeEventHub`, que también enviaba
PING periódicos.

### 8.3 `/status`

Devuelve el número local de subscribers activos y estado `ONLINE`.

El contador es **por instancia del backend**, no global de todo un cluster.

### 8.4 `/emit`

Es una utilidad de diagnóstico/desarrollo restringida a `ADMINISTRADOR`.

No es el camino productivo principal. El camino productivo debe originarse en un caso de uso a
través de `RealtimePublisherPort`.

Si `data` no es un objeto JSON, el endpoint responde `400` en vez de producir un
`ClassCastException`/`500`.

---

## 9. Seguridad y consumo Angular

`SecurityConfig` mantiene:

```text
GET  /api/v1/realtime/**  -> authenticated
POST /api/v1/realtime/emit -> ADMINISTRADOR
```

El `EventSource` nativo del navegador no permite establecer libremente
`Authorization: Bearer ...`.

Por tanto el frontend debe utilizar una estrategia que soporte headers, por ejemplo:

- `fetch` + `ReadableStream`; o
- un cliente SSE que permita enviar `Authorization`.

No se debe resolver este problema con token en URL ni haciendo público el stream.

El token debe obtenerse y renovarse con el flujo Keycloak ya definido para el frontend. La
reconexión del stream debe usar un access token vigente.

---

## 10. Eventos de negocio

El contrato vigente del lote y su payload se mantiene en [REALTIME_EVENT_STANDARD](../contracts/REALTIME_EVENT_STANDARD.md). El use case individual conserva el evento `ASISTENCIA_REGISTRADA` con `estudiante`, `grupo`, `sesion`, `presente`, pero el adapter SQL de esa operación lanza `FeatureUnavailableException`; no se presenta como camino productivo certificado. El Golden Path actual es el lote, con publicación posterior al éxito de persistencia.

---

## 11. Pruebas y reglas de arquitectura

La suite cubre al menos:

- un subscriber recibe;
- varios subscribers reciben;
- orden para consumidor disponible;
- publicación sin subscribers se descarta sin excepción;
- un evento emitido antes de conectar no se replayea;
- desconectar un cliente no impide conexiones futuras;
- enriquecimiento de `correlationId`;
- métricas publish/drop/gauge;
- un consumidor sin demanda no bloquea a otro disponible;
- mapeo a `ServerSentEvent`;
- heartbeat;
- seguridad 401/403/200;
- wiring condicionado del provider `local-sse`;
- publicación después de persistencia;
- no publicación ante error de persistencia.

ArchUnit protege explícitamente que Application no dependa de:

```text
reactor..
org.reactivestreams..
org.springframework.web.reactive..
org.springframework.http.codec..
```

Gates efectivos y diferencia entre checks locales/remotos: [TESTING_STANDARD](../testing/TESTING_STANDARD.md). No inferir el umbral remoto de Sonar ni una corrida exitosa desde esta arquitectura.

---

## 12. Migración desde `RealtimeEventHub`

La implementación anterior basada en `SseEmitter` fue retirada.

Cambios intencionales:

| Antes | Ahora |
|---|---|
| `ConcurrentHashMap<String,SseEmitter>` | `Sinks.Many<RealtimeEvent>` |
| broadcast manual | `Flux` multicast |
| PING programado global | heartbeat reactivo por conexión |
| `clientId` técnico | identidad de evento + autenticación existente |
| estado dentro del controller package | provider detrás de Composition Root |
| sin puerto Application | `RealtimePublisherPort` |

No se conserva replay de eventos ni un registro de `clientId`; ninguno formaba parte de un
contrato de negocio durable.

---

## 13. Limitaciones y bloqueos conocidos

### 13.1 Provider local a una sola instancia

`ReactorRealtimeAdapter` vive en memoria de **una JVM**. Si se levantan dos instancias:

```text
backend A -> subscribers de A
backend B -> subscribers de B
```

un evento publicado en A no aparece automáticamente en B.

**Bloqueo para escalar horizontalmente:** validar un mecanismo distribuido aprobado mediante ADR (LB-005).

### 13.2 Sin durabilidad

Un usuario desconectado pierde eventos. La UI no debe considerar SSE como fuente de verdad.
Después de reconectar debe poder refrescar estado desde endpoints de consulta.

### 13.3 Autenticación del navegador

El checkout no incluye el frontend: su consumo SSE con Bearer, reconexión y refresh no se certifica aquí. Hasta evidencia E2E no se cierra la integración frontend↔realtime; [TD-017](../baseline/TECHNICAL_DEBT.md#td-017).

### 13.4 Spring MVC sigue siendo servlet

El stream usa una fuente reactiva y backpressure, pero las escrituras HTTP de MVC son bloqueantes
y se delegan al executor asíncrono. Actualmente Boot usa virtual threads por configuración.
Migrar todo a WebFlux no está justificado por esta fase.

### 13.5 Métrica de pérdida por subscriber

`directBestEffort` no reporta al publisher la pérdida individual de un cliente lento cuando otro
subscriber sí aceptó el evento. La métrica `dropped` solo representa rechazos globales del
`tryEmitNext`.

### 13.6 Endpoint `/emit`

Debe seguir restringido a `ADMINISTRADOR`. Antes de producción se debe decidir si:

- se conserva como diagnóstico administrativo;
- se protege adicionalmente con una property;
- o se elimina.

No debe utilizarse como bypass de casos de uso.

### 13.7 Sin contrato API formal todavía

La siguiente fase API First / Contract First debe describir formalmente el stream SSE, los
eventos y las respuestas HTTP. Este documento es arquitectura, no reemplaza OpenAPI.

---

## 14. Evolución y cierre

La distribución futura conserva puertos neutrales y requiere ADR antes de seleccionar tecnología. RabbitMQ/Redis/WebSocket no se incorporan por una mención histórica. Secuencia activa: [LINEA_BASE](../baseline/LINEA_BASE.md); estado de deuda: [TD-003](../baseline/TECHNICAL_DEBT.md#td-003), [TD-017](../baseline/TECHNICAL_DEBT.md#td-017), [TD-018](../baseline/TECHNICAL_DEBT.md#td-018); calidad y cierre: [DoD única](../baseline/DEFINITION_OF_DONE.md).
