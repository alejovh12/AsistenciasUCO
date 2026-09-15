package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse;

import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import co.edu.uco.asistenciasuco.infrastructure.observability.tracing.opentelemetry.TraceContextSnapshot;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adaptador local best-effort que implementa {@link RealtimePublisherPort} sobre Project Reactor.
 *
 * <p>Este adapter es tecnologia de infraestructura y no se autoregistra como componente Spring:
 * el Composition Root {@code LocalSseRealtimeAdapterConfiguration} decide si debe existir según
 * {@code app.adapters.realtime.provider=local-sse}.</p>
 *
 * <h2>Politica del sink</h2>
 * <p>Se usa {@code Sinks.many().multicast().directBestEffort()} porque el canal SSE local es
 * efimero: no ofrece durabilidad ni replay. Sin suscriptores, {@code tryEmitNext} falla de forma
 * inmediata y el evento se descarta de manera observable. Si un suscriptor no tiene demanda,
 * ese cliente puede perder el evento sin frenar a los consumidores que si estan listos.</p>
 *
 * <h2>Resiliencia</h2>
 * <p>Realtime es secundario respecto a la operacion de negocio. Un resultado de emision distinto
 * de {@code OK} se registra y contabiliza, pero nunca se propaga como excepcion al UseCase.</p>
 */
public final class ReactorRealtimeAdapter implements RealtimePublisherPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorRealtimeAdapter.class);

    private final Sinks.Many<RealtimeEvent> sink = Sinks.many().multicast().directBestEffort();
    private final AtomicInteger activeSubscribers = new AtomicInteger();
    private final Flux<RealtimeEvent> events;
    private final Counter publishedCounter;
    private final Counter droppedCounter;

    public ReactorRealtimeAdapter(final MeterRegistry meterRegistry) {
        Objects.requireNonNull(meterRegistry, "MeterRegistry es obligatorio.");

        this.events = sink.asFlux()
                .doOnSubscribe(subscription -> activeSubscribers.incrementAndGet())
                .doFinally(signal -> activeSubscribers.decrementAndGet());

        Gauge.builder("realtime.subscribers.active", activeSubscribers, AtomicInteger::get)
                .description("Numero de clientes SSE actualmente conectados al canal realtime.")
                .register(meterRegistry);

        this.publishedCounter = Counter.builder("realtime.events.published")
                .description("Eventos realtime aceptados por el sink local para al menos un consumidor.")
                .register(meterRegistry);

        this.droppedCounter = Counter.builder("realtime.events.dropped")
                .description("Intentos realtime rechazados por el sink local, por ejemplo sin suscriptores o por concurrencia.")
                .register(meterRegistry);
    }

    @Override
    public void publish(final RealtimeEvent event) {
        if (event == null) {
            droppedCounter.increment();
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
     * Flujo local de eventos para la capa HTTP de infraestructura.
     *
     * <p>No forma parte de {@link RealtimePublisherPort}; Application nunca expone ni consume
     * {@link Flux}.</p>
     */
    public Flux<RealtimeEvent> events() {
        return events;
    }

    public int activeSubscribersCount() {
        return activeSubscribers.get();
    }
}
