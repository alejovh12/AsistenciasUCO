package co.edu.uco.notificaciones.infrastructure.adapter.out.smtp;

import co.edu.uco.notificaciones.domain.CorreoSaliente;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class LogCorreoTransportTest {

    @Test
    void estaActivoYNoFallaAlEnviar() {
        final LogCorreoTransport t = new LogCorreoTransport();
        assertTrue(t.activo());
        assertDoesNotThrow(() -> t.enviar(new CorreoSaliente("a@uco.edu.co", "", "a@uco.edu.co", List.of("b@uco.edu.co"), "A", "t", "h", "<id>")));
    }
}
