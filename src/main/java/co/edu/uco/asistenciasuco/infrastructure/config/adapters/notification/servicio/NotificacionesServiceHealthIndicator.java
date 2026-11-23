package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.servicio;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.service.NotificacionesServiceClient;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

import java.util.Objects;

/**
 * Salud del Servicio de Notificaciones ({@code GET /actuator/health/readiness} del servicio). Se publica en
 * {@code /actuator/health} (componente {@code notificacionesService}) pero no en el grupo {@code readiness} de esta API: si el
 * servicio cae, el outbox reintenta y la API sigue sirviendo. Los detalles nunca incluyen mensajes del servicio.
 */
public final class NotificacionesServiceHealthIndicator implements HealthIndicator {

    private final NotificacionesServiceClient cliente;

    public NotificacionesServiceHealthIndicator(final NotificacionesServiceClient cliente) {
        this.cliente = Objects.requireNonNull(cliente, "NotificacionesServiceClient es obligatorio.");
    }

    @Override
    public Health health() {
        return cliente.readiness() ? Health.up().build() : Health.down().withDetail("motivo", "readiness no responde UP").build();
    }
}
