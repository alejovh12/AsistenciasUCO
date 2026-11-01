package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class LogNotificacionPrivacidadTest {
    @Test
    void descripcion_y_log_omiten_valores_personales_y_campos_desconocidos() throws Exception {
        final UUID usuario = UUID.randomUUID();
        final Map<String, String> datos = Map.ofEntries(
                Map.entry("nombre", "FAKE_NOMBRE"), Map.entry("apellido", "FAKE_APELLIDO"),
                Map.entry("correo", "fake.correo@example.test"), Map.entry("telefono", "FAKE_TELEFONO"),
                Map.entry("documento", "FAKE_DOCUMENTO"), Map.entry("token", "FAKE_TOKEN"),
                Map.entry("password", "FAKE_PASSWORD"), Map.entry("motivo", "FAKE_MOTIVO"),
                Map.entry("campoDesconocido", "FAKE_CAMPO"));
        final var aviso = new Notificacion("VERIFICACION_CORREO", "FAKE_ASUNTO", "FAKE_MENSAJE", List.of(new Notificacion.Destinatario("ESTUDIANTE", usuario, "FAKE_NOMBRE", "fake.correo@example.test")), datos);
        final var adaptador = new LogNotificacionAdapter();
        final Logger logger = (Logger) LoggerFactory.getLogger(LogNotificacionAdapter.class);
        final ListAppender<ILoggingEvent> captura = new ListAppender<>();
        captura.start();
        logger.addAppender(captura);
        try {
            final String descripcion = adaptador.describir(aviso);
            adaptador.notificar(aviso);
            assertFalse(captura.list.isEmpty());
            final String registrado = captura.list.getLast().getFormattedMessage();
            for (final String valor : List.of("FAKE_NOMBRE", "FAKE_APELLIDO", "fake.correo@example.test",
                    "FAKE_TELEFONO", "FAKE_DOCUMENTO", "FAKE_TOKEN", "FAKE_PASSWORD",
                    "FAKE_MOTIVO", "FAKE_CAMPO", "FAKE_ASUNTO", "FAKE_MENSAJE", "campoDesconocido")) {
                assertFalse(descripcion.contains(valor), "La descripción debe omitir datos privados o desconocidos");
                assertFalse(registrado.contains(valor), "El log debe omitir datos privados o desconocidos");
            }
            assertTrue(registrado.contains("VERIFICACION_CORREO"));
            assertTrue(registrado.contains(usuario.toString()));
        } finally {
            logger.detachAppender(captura);
            captura.stop();
        }
    }
}
