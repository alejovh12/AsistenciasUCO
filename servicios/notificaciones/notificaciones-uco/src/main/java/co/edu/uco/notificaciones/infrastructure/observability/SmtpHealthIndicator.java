package co.edu.uco.notificaciones.infrastructure.observability;

import co.edu.uco.notificaciones.application.model.ConfiguracionCanal;
import co.edu.uco.notificaciones.application.port.out.CorreoTransportPort;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** Salud del servidor SMTP (abre y cierra una conexion). Solo aporta a /actuator/health; no afecta la sonda de readiness. */
@Component("smtp")
public class SmtpHealthIndicator implements HealthIndicator {

    private final CorreoTransportPort transporte;
    private final ConfiguracionCanal canal;

    public SmtpHealthIndicator(final CorreoTransportPort transporte, final ConfiguracionCanal canal) {
        this.transporte = Objects.requireNonNull(transporte);
        this.canal = Objects.requireNonNull(canal);
    }

    @Override
    public Health health() {
        try {
            transporte.verificarConexion();
            return Health.up().withDetail("host", canal.host()).withDetail("port", canal.port()).build();
        } catch (final UnsupportedOperationException ex) {
            return Health.unknown().withDetail("canal", canal.canal()).build();
        } catch (final Exception fallo) {
            return Health.down().withDetail("host", canal.host()).withDetail("port", canal.port())
                    .withDetail("error", fallo.getClass().getSimpleName()).build();
        }
    }
}
