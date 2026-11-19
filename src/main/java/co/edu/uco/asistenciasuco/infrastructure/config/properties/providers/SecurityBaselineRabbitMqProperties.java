package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;
import java.util.UUID;

/**
 * SEC-BASELINE-020: identidad y topologia del consumidor de eventos de asignacion del PDP en RabbitMQ (exchange,
 * cola y el {@code applicationId} propio de {@code asistencias-uco} con que el PDP enruta sus eventos). Solo se
 * enlaza con {@code app.adapters.assignment-events.enabled=true} (ver {@code AssignmentEventsRabbitConfiguration}).
 */
@ConfigurationProperties(prefix = "app.providers.security-baseline-rabbitmq")
public record SecurityBaselineRabbitMqProperties(UUID applicationId, String exchange, String queue) {

    public SecurityBaselineRabbitMqProperties {
        Objects.requireNonNull(applicationId, "app.providers.security-baseline-rabbitmq.application-id es obligatorio.");
        if (exchange == null || exchange.isBlank()) {
            throw new IllegalStateException("app.providers.security-baseline-rabbitmq.exchange es obligatorio.");
        }
        if (queue == null || queue.isBlank()) {
            throw new IllegalStateException("app.providers.security-baseline-rabbitmq.queue es obligatorio.");
        }
    }
}
