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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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

    /** OBS-07: hechos de negocio publicados, por tipo ({@code asistencias_negocio_eventos_total{tipo}} en Prometheus). */
    static final String METRICA_EVENTOS_NEGOCIO = "asistencias.negocio.eventos";
    /**
     * OBS-07: tipos de evento que cuentan como hecho de negocio. Lista cerrada para acotar la cardinalidad: los eventos tecnicos
     * (rotacion del QR, cambios de catalogos) no crean series.
     */
    static final Set<String> TIPOS_NEGOCIO = Set.of(
            "TOMA_ABIERTA", "TOMA_CERRADA", "ASISTENCIAS_TOMA_ACTUALIZADAS", "ASISTENCIA_QR_REGISTRADA",
            "RECLAMO_ASISTENCIA_RADICADO", "RECLAMO_ASISTENCIA_RESUELTO", "SESION_CANCELADA", "SESION_MANUAL_CREADA",
            "REVISION_CANCELACION_RESUELTA", "ALERTA_RIESGO_INASISTENCIA", "ALERTA_PERDIDA_INASISTENCIA"
    );

    private final Sinks.Many<RealtimeEvent> sink = Sinks.many().multicast().directBestEffort();
    private final AtomicInteger activeSubscribers = new AtomicInteger();
    private final Flux<RealtimeEvent> events;
    private final Counter publishedCounter;
    private final Counter droppedCounter;
    private final Counter noSubscribersCounter;
    private final MeterRegistry meterRegistry;
    private final List<Consumer<RealtimeEvent>> observadores = new CopyOnWriteArrayList<>();

    public ReactorRealtimeAdapter(final MeterRegistry meterRegistry) {
        Objects.requireNonNull(meterRegistry, "MeterRegistry es obligatorio.");
        this.meterRegistry = meterRegistry;

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

        this.noSubscribersCounter = Counter.builder("realtime.events.no_subscribers")
                .description("Eventos realtime publicados cuando no habia ningun cliente SSE conectado (subconjunto de los descartados).")
                .register(meterRegistry);
    }

    @Override
    public void publish(final RealtimeEvent event) {
        if (event == null) {
            droppedCounter.increment();
            LOGGER.warn("Se intento publicar un evento realtime nulo; operacion ignorada.");
            return;
        }

        contarEventoDeNegocio(event.type());

        final TraceContextSnapshot traceContext = TraceContextSnapshot.current();
        final RealtimeEvent enriched = event.withObservabilityContext(
                CorrelationIdContext.getAsString(),
                traceContext.traceId(),
                traceContext.spanId()
        );

        emitir(enriched);
    }

    /**
     * REDIS-001: entrega a los clientes SSE de esta replica un evento que otra replica (o esta misma) ya publico y relevo
     * por Redis. No lo vuelve a contar como hecho de negocio ni reemplaza su contexto de trazabilidad: ambos se fijaron
     * al publicarlo en la replica de origen.
     */
    public void entregarRelevado(final RealtimeEvent event) {
        if (event == null) {
            droppedCounter.increment();
            return;
        }
        emitir(event);
    }

    /**
     * REDIS-001: fija el contexto de correlacion y trazabilidad del hilo actual y cuenta el hecho de negocio, sin emitir.
     * Lo usa el relevo Redis antes de enviar el evento a las demas replicas.
     */
    public RealtimeEvent prepararParaRelevo(final RealtimeEvent event) {
        Objects.requireNonNull(event, "El evento realtime es obligatorio.");
        contarEventoDeNegocio(event.type());
        final TraceContextSnapshot traceContext = TraceContextSnapshot.current();
        return event.withObservabilityContext(CorrelationIdContext.getAsString(), traceContext.traceId(), traceContext.spanId());
    }

    /**
     * REDIS-001: registra un observador interno que recibe cada evento que esta replica entrega (publicado aqui o relevado
     * desde otra). No cuenta como cliente SSE. Un fallo del observador se registra y nunca afecta la entrega.
     */
    public void alEntregar(final Consumer<RealtimeEvent> observador) {
        observadores.add(Objects.requireNonNull(observador, "El observador es obligatorio."));
    }

    private void emitir(final RealtimeEvent enriched) {
        notificarObservadores(enriched);
        final Sinks.EmitResult result = sink.tryEmitNext(enriched);
        if (result.isSuccess()) {
            publishedCounter.increment();
            return;
        }

        droppedCounter.increment();
        if (result == Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
            // Sin suscriptores no es un fallo: nadie escucha ese canal. Se cuenta, pero no ensucia el log por cada evento.
            noSubscribersCounter.increment();
            LOGGER.debug("Evento realtime sin suscriptores. type={} eventId={} correlationId={}",
                    enriched.type(), enriched.eventId(), enriched.correlationId());
            return;
        }
        LOGGER.warn(
                "Evento realtime descartado. type={} eventId={} correlationId={} emitResult={}",
                enriched.type(), enriched.eventId(), enriched.correlationId(), result
        );
    }

    private void notificarObservadores(final RealtimeEvent event) {
        for (final Consumer<RealtimeEvent> observador : observadores) {
            try {
                observador.accept(event);
            } catch (final RuntimeException exception) {
                LOGGER.warn("Observador realtime fallo. type={} eventId={} causa={}", event.type(), event.eventId(),
                        exception.getClass().getSimpleName());
            }
        }
    }

    private void contarEventoDeNegocio(final String tipo) {
        if (tipo != null && TIPOS_NEGOCIO.contains(tipo)) {
            Counter.builder(METRICA_EVENTOS_NEGOCIO)
                    .description("Hechos de negocio publicados (tomas, registros por QR, reclamos, sesiones y alertas).")
                    .tag("tipo", tipo)
                    .register(meterRegistry)
                    .increment();
        }
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
