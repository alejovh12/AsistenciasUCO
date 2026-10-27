package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Representacion inmutable de una notificacion leida del outbox para despacho.
 */
public record NotificacionPersistida(
        UUID id,
        String tipo,
        String asunto,
        String mensaje,
        List<DestinatarioRef> destinatarios,
        Map<String, String> datos,
        int intentos,
        Instant creadoEn
) {

    public NotificacionPersistida {
        destinatarios = destinatarios == null ? List.of() : List.copyOf(destinatarios);
        datos = datos == null ? Map.of() : Map.copyOf(datos);
    }
}
