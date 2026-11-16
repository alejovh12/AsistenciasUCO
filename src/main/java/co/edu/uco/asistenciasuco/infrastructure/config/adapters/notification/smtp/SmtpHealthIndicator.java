package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.smtp;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp.CorreoTransport;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

import java.util.Objects;

/**
 * OBS-10 (VAL-oleada8): salud del servidor SMTP del canal de notificaciones. Abre y cierra una conexion sin enviar nada.
 * <p>
 * Se publica en {@code /actuator/health} (componente {@code smtp}) pero NO en el grupo {@code readiness}: si el SMTP cae, el
 * outbox reintenta y la API sigue sirviendo; sacar la replica del balanceo seria peor. Los detalles nunca llevan el mensaje
 * de la excepcion (puede incluir el usuario o la respuesta del servidor), solo su tipo.
 */
public final class SmtpHealthIndicator implements HealthIndicator {

    private final CorreoTransport transport;
    private final String host;
    private final int port;

    public SmtpHealthIndicator(final CorreoTransport transport, final String host, final int port) {
        this.transport = Objects.requireNonNull(transport, "CorreoTransport es obligatorio.");
        this.host = Objects.requireNonNull(host, "El host SMTP es obligatorio.");
        this.port = port;
    }

    @Override
    public Health health() {
        try {
            transport.verificarConexion();
            return Health.up().withDetail("host", host).withDetail("port", port).build();
        } catch (final UnsupportedOperationException sinVerificacion) {
            return Health.unknown().withDetail("host", host).withDetail("port", port).build();
        } catch (final Exception fallo) {
            return Health.down()
                    .withDetail("host", host)
                    .withDetail("port", port)
                    .withDetail("error", fallo.getClass().getSimpleName())
                    .build();
        }
    }
}
