package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador de {@link NotificacionPort} que persiste avisos institucionales en la tabla outbox
 * de SQL Server de forma asincrona y durable, despertando al despachador en segundo plano.
 */
public class OutboxNotificacionAdapter implements NotificacionPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxNotificacionAdapter.class);

    private final NotificacionOutboxRepository repository;
    private final NotificacionOutboxDispatcher dispatcher;
    private final ObjectMapper objectMapper;

    private final Counter acceptedCounter;
    private final Counter skippedCounter;
    private final Counter failedCounter;

    public OutboxNotificacionAdapter(
            final NotificacionOutboxRepository repository,
            final NotificacionOutboxDispatcher dispatcher,
            final MeterRegistry meterRegistry,
            final ObjectMapper objectMapper
    ) {
        this.repository = Objects.requireNonNull(repository, "NotificacionOutboxRepository es obligatorio.");
        this.dispatcher = Objects.requireNonNull(dispatcher, "NotificacionOutboxDispatcher es obligatorio.");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper es obligatorio.");
        Objects.requireNonNull(meterRegistry, "MeterRegistry es obligatorio.");

        this.acceptedCounter = Counter.builder("notification.publish.total")
                .tag("provider", "outbox")
                .tag("outcome", "accepted")
                .register(meterRegistry);
        this.skippedCounter = Counter.builder("notification.publish.total")
                .tag("provider", "outbox")
                .tag("outcome", "skipped")
                .register(meterRegistry);
        this.failedCounter = Counter.builder("notification.publish.total")
                .tag("provider", "outbox")
                .tag("outcome", "failed")
                .register(meterRegistry);
    }

    @Override
    public void notificar(final Notificacion notificacion) {
        Objects.requireNonNull(notificacion, "La notificacion es obligatoria.");

        if (notificacion.destinatarios().isEmpty()) {
            skippedCounter.increment();
            return;
        }

        try {
            final List<DestinatarioRef> refs = notificacion.destinatarios().stream()
                    .map(d -> new DestinatarioRef(d.rol(), d.idUsuario()))
                    .toList();
            final String destinatariosJson = objectMapper.writeValueAsString(refs);

            final String datosJson = notificacion.datos().isEmpty()
                    ? "{}"
                    : objectMapper.writeValueAsString(notificacion.datos());

            if (datosJson.length() > 8000) {
                LOGGER.warn("Datos de notificacion exceden longitud maxima de 8000 caracteres. tipo={}", notificacion.tipo());
                failedCounter.increment();
                return;
            }

            final Span currentSpan = Span.current();
            final String traceId = currentSpan != null && currentSpan.getSpanContext().isValid()
                    ? currentSpan.getSpanContext().getTraceId()
                    : null;
            final String spanId = currentSpan != null && currentSpan.getSpanContext().isValid()
                    ? currentSpan.getSpanContext().getSpanId()
                    : null;

            final RegistroNotificacion registro = new RegistroNotificacion(
                    UUID.randomUUID(),
                    notificacion.tipo(),
                    notificacion.asunto(),
                    notificacion.mensaje(),
                    destinatariosJson,
                    datosJson,
                    CorrelationIdContext.get(),
                    traceId,
                    spanId
            );

            repository.registrar(registro);
            acceptedCounter.increment();
            dispatcher.signal();
        } catch (final JsonProcessingException e) {
            LOGGER.warn("Fallo de serializacion JSON al preparar notificacion outbox tipo={}", notificacion.tipo());
            failedCounter.increment();
        } catch (final RuntimeException e) {
            LOGGER.warn("Fallo al registrar notificacion outbox tipo={} excepcion={}", notificacion.tipo(), e.getClass().getSimpleName());
            failedCounter.increment();
        }
    }
}
