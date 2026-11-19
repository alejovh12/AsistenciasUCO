package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.messaging.rabbitmq;

import co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.common.dto.EventoAsignacionSeguridad;
import co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.primaryports.RegistrarEventoAsignacionInputPort;
import co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.primaryports.RegistrarEventoAsignacionInputPort.EventoAsignacionInvalidoException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

/**
 * SEC-BASELINE-020: adaptador primario de los eventos {@code assignment.changed.<applicationId>} del PDP (Security
 * Baseline 0.1.14), entregados por RabbitMQ con ACK manual (manual del PDP, seccion 6.4).
 *
 * <ul>
 *   <li>Payload no legible (no es JSON/objeto) -&gt; {@code reject} sin requeue: reintentar no lo arregla.</li>
 *   <li>Contrato invalido ({@code schemaVersion}/{@code applicationId}) -&gt; {@code reject} sin requeue.</li>
 *   <li>Procesado o duplicado -&gt; {@code ack}: la entrega al-menos-una-vez no debe repetir el efecto.</li>
 *   <li>Cualquier otra falla (p. ej. BD caida) -&gt; {@code nack} con requeue: el efecto todavia no ocurrio.</li>
 * </ul>
 */
public class AssignmentEventsRabbitListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssignmentEventsRabbitListener.class);

    private final RegistrarEventoAsignacionInputPort registrarEventoAsignacion;
    private final AssignmentChangedEventMapper mapper;

    public AssignmentEventsRabbitListener(
            final RegistrarEventoAsignacionInputPort registrarEventoAsignacion,
            final AssignmentChangedEventMapper mapper) {
        this.registrarEventoAsignacion = Objects.requireNonNull(registrarEventoAsignacion,
                "RegistrarEventoAsignacionInputPort es obligatorio.");
        this.mapper = Objects.requireNonNull(mapper, "AssignmentChangedEventMapper es obligatorio.");
    }

    @RabbitListener(queues = "${app.providers.security-baseline-rabbitmq.queue}", ackMode = "MANUAL")
    public void consumir(final Message message, final Channel channel, @Header("amqp_deliveryTag") final long tag)
            throws IOException {
        procesar(message.getBody(), channel, tag);
    }

    void procesar(final byte[] body, final Channel channel, final long tag) throws IOException {
        final EventoAsignacionSeguridad evento;
        try {
            evento = mapper.leer(body);
        } catch (final RuntimeException e) {
            LOGGER.warn("Evento de asignacion ilegible; se rechaza sin reintentar. causa={}", e.getClass().getSimpleName());
            channel.basicReject(tag, false);
            return;
        }
        CorrelationIdContext.set(correlationId(evento));
        try {
            final RegistrarEventoAsignacionInputPort.ResultadoRegistro resultado = registrarEventoAsignacion.procesar(evento);
            LOGGER.info("Evento de asignacion procesado. eventId={}, eventType={}, resultado={}",
                    evento.eventId(), evento.eventType(), resultado);
            channel.basicAck(tag, false);
        } catch (final EventoAsignacionInvalidoException e) {
            LOGGER.warn("Evento de asignacion con contrato invalido; se rechaza sin reintentar. eventId={}, causa={}",
                    evento.eventId(), e.getMessage());
            channel.basicReject(tag, false);
        } catch (final RuntimeException e) {
            LOGGER.warn("Evento de asignacion no procesado; se reintentara. eventId={}, causa={}",
                    evento.eventId(), e.getClass().getSimpleName());
            channel.basicNack(tag, false, true);
        } finally {
            CorrelationIdContext.clear();
        }
    }

    private UUID correlationId(final EventoAsignacionSeguridad evento) {
        if (evento.correlationId() != null) {
            try {
                return UUID.fromString(evento.correlationId());
            } catch (final IllegalArgumentException ignored) {
                // sigue con una correlacion propia; el correlationId ajeno no es un UUID valido.
            }
        }
        return UUID.randomUUID();
    }
}
