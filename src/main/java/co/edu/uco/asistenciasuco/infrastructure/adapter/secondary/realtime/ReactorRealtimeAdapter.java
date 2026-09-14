package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime;

import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import co.edu.uco.asistenciasuco.infrastructure.observability.tracing.TraceContextSnapshot;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adaptador tecnologico que implementa {@link RealtimePublisherPort} sobre Project Reactor.
 *
 * <h2>Eleccion del Sink</h2>
 * <p>Se usa {@code Sinks.many().multicast().onBackpressureBuffer(BUFFER_SIZE, autoCancel=false)}:</p>
 * <ul>
 *   <li><b>multicast</b>: cada evento publicado se distribuye a TODOS los suscriptores activos
 *       (multiples pestañas/usuarios viendo el mismo canal), no solo a uno.</li>
 *   <li><b>onBackpressureBuffer(256, false)</b>: buffer ACOTADO (nunca ilimitado) por suscriptor
 *       lento, para no bloquear el hilo que publica ({@link #publish(RealtimeEvent)} nunca
 *       bloquea) ni crecer memoria sin limite. Cuando el buffer se agota (suscriptor
 *       desconectado/lento o ausencia total de suscriptores) {@code tryEmitNext} deja de tener
 *       exito; la politica adoptada aqui es: log + metrica + descarte controlado del evento,
 *       nunca una excepcion que rompa la operacion de negocio que disparo la publicacion.</li>
 *   <li><b>autoCancel=false</b>: el sink es un bean singleton de larga vida (mientras dure el
 *       proceso); que todos los suscriptores se desconecten momentaneamente no debe terminar el
 *       sink ni impedir que futuros suscriptores se conecten.</li>
 * </ul>
 *
 * <h2>Manejo de {@link Sinks.EmitResult}</h2>
 * <p>Todo resultado distinto de {@code OK} (incluye {@code FAIL_OVERFLOW},
 * {@code FAIL_NON_SERIALIZED}, {@code FAIL_TERMINATED}, {@code FAIL_CANCELLED},
 * {@code FAIL_ZERO_SUBSCRIBER}) se registra explicitamente: se loguea en WARN y se incrementa
 * {@code realtime_events_dropped_total}. Nunca se relanza como excepcion.</p>
 *
 * <h2>Enriquecimiento de observabilidad</h2>
 * <p>El {@link RealtimeEvent} que llega desde Application no trae {@code correlationId}/
 * {@code traceId}/{@code spanId} (Application no tiene acceso a infraestructura). Este adaptador
 * los resuelve del contexto de observabilidad vigente (hilo del request HTTP que disparo la
 * operacion de negocio) justo antes de emitir.</p>
 */
@Component
public final class ReactorRealtimeAdapter implements RealtimePublisherPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorRealtimeAdapter.class);

    /** Tamaño acotado del buffer de backpressure por suscriptor. Ver Javadoc de la clase. */
    static final int BUFFER_SIZE = 256;

    private final Sinks.Many<RealtimeEvent> sink =
            Sinks.many().multicast().onBackpressureBuffer(BUFFER_SIZE, false);
    private final AtomicInteger activeSubscribers = new AtomicInteger(0);
    private final Flux<RealtimeEvent> events;
    private final Counter publishedCounter;
    private final Counter droppedCounter;

    public ReactorRealtimeAdapter(final MeterRegistry meterRegistry) {
        Objects.requireNonNull(meterRegistry, "MeterRegistry es obligatorio.");
        this.events = sink.asFlux()
                .doOnSubscribe(subscription -> activeSubscribers.incrementAndGet())
                .doFinally(signal -> activeSubscribers.decrementAndGet());
        Gauge.builder("realtime_subscribers_active", activeSubscribers, AtomicInteger::get)
                .description("Numero de clientes SSE actualmente conectados al canal realtime.")
                .register(meterRegistry);
        this.publishedCounter = Counter.builder("realtime_events_published_total")
                .description("Eventos realtime publicados exitosamente en el sink.")
                .register(meterRegistry);
        this.droppedCounter = Counter.builder("realtime_events_dropped_total")
                .description("Eventos realtime descartados por fallo de emision (overflow, sin suscriptores, sink terminado, etc.).")
                .register(meterRegistry);
    }

    @Override
    public void publish(final RealtimeEvent event) {
        if (event == null) {
            LOGGER.warn("Se intento publicar un evento realtime nulo; operacion ignorada.");
            return;
        }
        final TraceContextSnapshot traceContext = TraceContextSnapshot.current();
        final RealtimeEvent enriched = event.withObservabilityContext(
                CorrelationIdContext.getAsString(),
                traceContext.traceId(),
                traceContext.spanId()
        );
        final Sinks.EmitResult result = sink.tryEmitNext(enriched);
        if (result.isSuccess()) {
            publishedCounter.increment();
            return;
        }
        droppedCounter.increment();
        LOGGER.warn(
                "Evento realtime descartado. type={} eventId={} correlationId={} emitResult={}",
                event.type(), event.eventId(), enriched.correlationId(), result
        );
    }

    /**
     * Flujo reactivo de eventos, consumido exclusivamente por el controlador SSE
     * (infraestructura). No forma parte de {@link RealtimePublisherPort}: Application nunca
     * debe depender de {@link Flux}.
     */
    public Flux<RealtimeEvent> events() {
        return events;
    }

    public int activeSubscribersCount() {
        return activeSubscribers.get();
    }
}
