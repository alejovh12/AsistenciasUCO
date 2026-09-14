# Reactividad realtime (Fase 3.1)

Este documento explica la vertical de eventos en tiempo real introducida en
`feature/reactive-realtime`: por que existe, donde viven sus piezas y como evoluciona.

## 1. Por que usamos Reactor solo para realtime

El backend es, y sigue siendo, **Spring MVC + JDBC bloqueante**. Reactor Core se introduce
unicamente para el problema que realmente lo necesita: mantener un canal *push*
(Server-Sent Events) abierto con multiples clientes concurrentes sin bloquear hilos del
servlet container ni recurrir a polling. Fuera de esa vertical, el resto de la aplicacion
(controllers CRUD, use cases, adapters JDBC) no cambia.

No se agrego `spring-boot-starter-webflux`. Spring MVC (`spring-webmvc`) ya sabe adaptar un
`Flux<T>` como tipo de retorno de un `@RestController` (via `ReactiveTypeHandler`, presente en
`spring-webmvc` desde Spring 5) siempre que Reactor Core este en el classpath; y
`org.springframework.http.codec.ServerSentEvent` vive en `spring-web` (no en `spring-webflux`).
Es decir: **Reactor Core + Spring MVC bastan** para `Flux<ServerSentEvent<T>>`. Agregar WebFlux
completo habria significado un segundo modelo de servidor (Netty reactivo) conviviendo con el
Tomcat/servlet actual, sin ningun beneficio para esta vertical.

## 2. Por que JDBC sigue bloqueante

Los repositorios JDBC (`NamedParameterJdbcOperations`, stored procedures canonicos, etc.) no se
tocan. Persisten datos transaccionales sobre SQL Server; envolverlos en reactividad no los hace
mas eficientes, solo mas complejos (ver punto 3).

## 3. Por que no hacemos "fake reactive wrappers"

Esta prohibido explicitamente:

```java
// NO HACER — falsa reactividad
Mono.fromCallable(() -> jdbcTemplate.query(...));
```

Envolver una llamada JDBC bloqueante en `Mono`/`Flux` no la vuelve no bloqueante: el hilo que
ejecuta el `Callable` se sigue bloqueando esperando la base de datos; solo se agrega la
sobrecarga y la complejidad del modelo reactivo sin ganar nada. La reactividad real requeriria
un driver no bloqueante (R2DBC), que esta fuera de alcance de esta fase (ver seccion 10).

## 4. Arquitectura de `RealtimePublisherPort`

```
Business Use Case (Application)
       |
       v
RealtimePublisherPort   <-- interfaz, vive en application.secondaryports.realtime
       |
       v
ReactorRealtimeAdapter  <-- implementacion, vive en infrastructure.adapter.secondary.realtime
       |
       v
Sinks.Many<RealtimeEvent>
       |
       v
Flux<RealtimeEvent>
       |
       v
SSE Controller (infrastructure.adapter.primary.controller.realtime)
       |
       v
Frontend Angular (fase siguiente)
```

`RealtimePublisherPort` (Application) expresa unicamente la intencion:

```java
public interface RealtimePublisherPort {
    void publish(RealtimeEvent event);
}
```

`RealtimeEvent` (Application) es un record neutral, sin ninguna dependencia de Reactor, SSE ni
OpenTelemetry:

```java
public record RealtimeEvent(
        UUID eventId, String type, Instant occurredAt,
        String correlationId, String traceId, String spanId,
        Map<String, Object> payload
) { ... }
```

`correlationId`/`traceId`/`spanId` quedan `null` cuando el use case construye el evento (vía
`RealtimeEvent.of(type, payload)`): Application no tiene forma de resolverlos, porque
`CorrelationIdContext` y el contexto de OpenTelemetry son infraestructura. El adaptador los
completa (ver seccion 7) justo antes de emitir.

`payload` es un `Map<String, Object>` de datos de negocio no sensibles (identificadores, nombres
de estado, banderas). **Nunca** debe contener tokens, contrasenas ni identificadores de sesion
de seguridad — esto se documenta en el Javadoc del record y se verifica en
`RegistrarAsistenciaUseCaseImplTest` (el payload publicado se limita a los campos de negocio
esperados).

Dos reglas ArchUnit (`application_no_depende_de_reactor`,
`application_no_depende_de_webflux_reactivo`) impiden que `co.edu.uco.asistenciasuco.application`
importe `reactor..`, `org.reactivestreams..`, `org.springframework.web.reactive..` o
`org.springframework.http.codec..`. Si algun dia alguien intenta usar `Mono`/`Flux` como tipo de
retorno de un use case, el build falla.

## 5. Adapter Reactor

`ReactorRealtimeAdapter implements RealtimePublisherPort` (unico, `@Component`, ciclo de vida de
singleton de Spring) usa:

```java
Sinks.many().multicast().onBackpressureBuffer(BUFFER_SIZE /* 256 */, /* autoCancel */ false)
```

Eleccion deliberada:

- **multicast**: cada evento se distribuye a *todos* los suscriptores activos simultaneamente
  (multiples pestañas/usuarios viendo el mismo canal), no a uno solo.
- **onBackpressureBuffer(256, false)**: buffer **acotado** por suscriptor lento. `publish()`
  nunca bloquea el hilo que lo invoca (usa `tryEmitNext`, no `emitNext` con reintentos). La
  memoria nunca crece sin limite: una vez lleno el buffer de un suscriptor que no consume,
  `tryEmitNext` deja de tener exito (`Sinks.EmitResult.FAIL_OVERFLOW`) en vez de seguir
  acumulando eventos. Este comportamiento esta verificado por una prueba reproducible
  (`ReactorRealtimeAdapterTest#overflow_del_buffer_se_maneja_sin_lanzar_excepcion...`) que
  satura el buffer con un suscriptor que nunca solicita elementos.
- **autoCancel = false**: el sink es un bean de larga vida; que todos los suscriptores se
  desconecten momentaneamente no debe terminarlo ni impedir que futuros clientes se conecten.

### Manejo de `Sinks.EmitResult`

`publish()` inspecciona **todo** el resultado de `tryEmitNext`. Si es `OK`, incrementa
`realtime_events_published_total`. Cualquier otro resultado (`FAIL_OVERFLOW`,
`FAIL_NON_SERIALIZED`, `FAIL_TERMINATED`, `FAIL_CANCELLED`, `FAIL_ZERO_SUBSCRIBER`) se registra
en un `LOGGER.warn(...)` estructurado (incluye `type`, `eventId`, `correlationId` y el
`EmitResult`) y se cuenta en `realtime_events_dropped_total`. **Nunca** se relanza como
excepcion: `RealtimePublisherPort#publish` documenta explicitamente ese contrato de resiliencia.

## 6. SSE

`RealtimeEventsController` (`infrastructure.adapter.primary.controller.realtime`) expone:

- `GET /api/v1/realtime/stream` → `Flux<ServerSentEvent<RealtimeEventResponse>>`,
  `produces = text/event-stream`. Cada elemento fija `id` (el `eventId`) y `event` (el `type`)
  del SSE, ademas del `data` JSON (`RealtimeEventResponse`, la proyeccion HTTP de
  `RealtimeEvent`).
- `GET /api/v1/realtime/status` → contador de suscriptores activos (`Gauge` Micrometer
  respaldado por un `AtomicInteger` incrementado/decrementado con `doOnSubscribe`/`doFinally`
  sobre el `Flux` compartido) y estado `ONLINE`.
- `POST /api/v1/realtime/emit` → utilidad de desarrollo, restringida a `ADMINISTRADOR` (ver
  seccion 8), NO es el unico punto de entrada de la reactividad (ver seccion 9).

La desconexion de un cliente (cierre de la conexion HTTP) cancela su suscripcion al `Flux`
(`doFinally`), decrementando el contador de activos sin afectar a otros suscriptores ni al sink
compartido — verificado en `ReactorRealtimeAdapterTest#desconexion_de_un_subscriptor_no_rompe_el_hub_para_otros`.

## 7. Backpressure

Ver seccion 5. En resumen: buffer acotado por suscriptor (256 eventos), `tryEmitNext` no
bloqueante, descarte controlado (log + metrica) cuando el buffer se agota, nunca crecimiento
ilimitado de memoria ni bloqueo de hilos.

## 8. Seguridad Bearer

`/api/v1/realtime/**` sigue exigiendo autenticacion igual que el resto de la API
(`SecurityConfig`, sin cambios): `GET /stream` y `GET /status` requieren cualquier usuario
autenticado; `POST /emit` requiere rol `ADMINISTRADOR`. **No se hizo publico el SSE, ni se
acepta el JWT por query param** (`/events?token=...`). Verificado en
`RbacSecurityFilterChainTest` (401 sin token, 403 con rol insuficiente en `/emit`, 200 con
Bearer valido en `/stream` para cualquier rol).

## 9. Por que Angular no debe usar `EventSource` nativo

El `EventSource` del navegador **no permite configurar headers** (no hay forma de enviar
`Authorization: Bearer ...`). Las alternativas incorrectas — hacer publico el endpoint o pasar el
token por query string — debilitan el backend y quedan explicitamente prohibidas en esta fase.
La solucion correcta, que se implementara en la fase Angular inmediatamente siguiente, es
consumir el stream via **`fetch` con `ReadableStream`** (o una libreria cliente SSE que soporte
headers, p. ej. `@microsoft/fetch-event-source`), enviando `Authorization: Bearer <token>` como
cualquier otra peticion autenticada del frontend. El backend ya esta listo para ese consumidor:
no requiere ningun cambio adicional del lado servidor.

## 10. Evolucion futura: RabbitMQ

Hoy:

```
UseCase → RealtimePublisherPort → Reactor (ReactorRealtimeAdapter) → SSE
```

Futuro (cuando el sistema necesite distribuir eventos entre multiples instancias del backend, o
persistir/reprocesar eventos):

```
UseCase → EventPublisherPort → RabbitMQ → Consumer → RealtimePublisherPort → Reactor → SSE
```

El punto clave: `RealtimePublisherPort` y `RealtimeEvent` (Application) no cambian. RabbitMQ
entraria como una nueva pieza de infraestructura *productora* (`EventPublisherPort`, otro puerto
de Application, implementado por un adapter RabbitMQ) que alimenta un *consumer* de
infraestructura; ese consumer seria quien invoque el `RealtimePublisherPort` existente
(`ReactorRealtimeAdapter`) para seguir emitiendo hacia SSE. Reactor nunca queda acoplado
directamente a RabbitMQ, ni RabbitMQ se filtra a Application: ambos son detalles de
infraestructura intercambiables detras de puertos neutrales, exactamente como Redis/MinIO cuando
se introduzcan.

## 11. Endpoint de negocio conectado hoy

`RegistrarAsistenciaUseCaseImpl` (registro de asistencia) publica `RealtimeEvent` de tipo
`ASISTENCIA_REGISTRADA` **solo despues** de que `AsistenciaRepositoryPort#registrarAsistencia`
haya terminado exitosamente. Un fallo de persistencia nunca llega a publicar evento (verificado
en `RegistrarAsistenciaUseCaseImplTest`); un fallo de emision realtime nunca revierte ni oculta
el registro ya persistido (no hay ninguna transaccion distribuida falsa: la persistencia SQL y
la publicacion realtime son dos pasos secuenciales independientes, el segundo con manejo de
error garantizado en el propio adapter).

`POST /api/v1/realtime/emit` se conserva como utilidad de desarrollo (restringida a
`ADMINISTRADOR`), pero no es la unica demostracion de reactividad del sistema.
