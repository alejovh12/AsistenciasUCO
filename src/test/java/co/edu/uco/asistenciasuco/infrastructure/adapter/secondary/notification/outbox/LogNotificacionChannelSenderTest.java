package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogNotificacionChannelSenderTest {

    private final LogNotificacionChannelSender sender = new LogNotificacionChannelSender();

    @Test
    @DisplayName("BE-S01 y BE-S02: describe tipo, rol:id, claves sin motivo ni datos sensibles")
    void BE_S01_y_S02_descripcion_segura() {
        final UUID id = UUID.randomUUID();
        final UUID usuarioId = UUID.randomUUID();
        final NotificacionPersistida persistida = new NotificacionPersistida(
                id,
                "RETIRO_ESTUDIANTE_GRUPO",
                "Asunto confidencial",
                "Mensaje privado",
                List.of(new DestinatarioRef("ESTUDIANTE", usuarioId)),
                Map.of("grupoId", "123", "motivo", "Inasistencias"),
                0,
                Instant.now()
        );

        final String desc = sender.describir(persistida);

        assertTrue(desc.contains(id.toString()));
        assertTrue(desc.contains("RETIRO_ESTUDIANTE_GRUPO"));
        assertTrue(desc.contains("ESTUDIANTE:" + usuarioId));
        assertFalse(desc.contains("grupoId=123"), "No debe registrar valores del mapa de datos");
        assertFalse(desc.contains("motivo="), "No debe registrar clave motivo");
        assertFalse(desc.contains("Asunto confidencial"), "No debe registrar asunto");
        assertFalse(desc.contains("Mensaje privado"), "No debe registrar mensaje");

        assertDoesNotThrow(() -> sender.enviar(persistida));
    }

    @Test
    @DisplayName("BE-S03: notificacion nula lanza NullPointerException")
    void BE_S03_nulo_lanza_npe() {
        assertThrows(NullPointerException.class, () -> sender.enviar(null));
    }
}
