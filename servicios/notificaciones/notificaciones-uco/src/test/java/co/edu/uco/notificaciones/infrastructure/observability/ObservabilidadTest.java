package co.edu.uco.notificaciones.infrastructure.observability;

import co.edu.uco.notificaciones.application.model.ConfiguracionCanal;
import co.edu.uco.notificaciones.application.port.out.CorreoTransportPort;
import co.edu.uco.notificaciones.domain.CorreoSaliente;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObservabilidadTest {

    private static final ConfiguracionCanal CANAL = new ConfiguracionCanal("smtp", "smtp.local", 587, "", false, "a@uco.edu.co", "n", true, false,
            50, 60, 500, "", false, List.of());

    @Test
    void metricasContarYMedirConEtiquetas() {
        final SimpleMeterRegistry registro = new SimpleMeterRegistry();
        final MicrometerMetricas metricas = new MicrometerMetricas(registro);
        metricas.contar("prueba.total", 2, "outcome", "sent");
        metricas.contar("prueba.total", 3, "outcome", "sent");
        metricas.medir("prueba.duracion", 5_000_000L, "outcome", "sent");
        metricas.medir("prueba.sin.tags", 1_000L);
        assertEquals(5.0, registro.get("prueba.total").tag("outcome", "sent").counter().count());
        assertEquals(1, registro.get("prueba.duracion").timer().count());
        assertEquals(1, registro.get("prueba.sin.tags").timer().count());
    }

    private static CorreoTransportPort transporte(final Exception fallo) {
        return new CorreoTransportPort() {
            @Override
            public void enviar(final CorreoSaliente correo) {
            }

            @Override
            public void verificarConexion() throws Exception {
                if (fallo != null) {
                    throw fallo;
                }
            }

            @Override
            public boolean activo() {
                return true;
            }
        };
    }

    @Test
    void saludSmtpArribaAbajoYDesconocido() {
        assertEquals(Status.UP, new SmtpHealthIndicator(transporte(null), CANAL).health().getStatus());
        final var abajo = new SmtpHealthIndicator(transporte(new java.io.IOException("caido")), CANAL).health();
        assertEquals(Status.DOWN, abajo.getStatus());
        assertEquals("IOException", abajo.getDetails().get("error"));
        final var desconocido = new SmtpHealthIndicator(transporte(new UnsupportedOperationException()), CANAL).health();
        assertEquals(Status.UNKNOWN, desconocido.getStatus());
    }
}
