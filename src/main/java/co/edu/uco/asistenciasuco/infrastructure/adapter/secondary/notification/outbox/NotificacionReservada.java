package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox;

import java.time.Instant;
import java.util.UUID;

/**
 * Fila devuelta por la reserva de notificaciones en el outbox.
 */
public record NotificacionReservada(
        UUID id,
        String tipo,
        String asunto,
        String mensaje,
        String destinatariosJson,
        String datosJson,
        int intentos,
        int maxIntentos,
        Instant creadoEn,
        String traceId,
        String spanId
) {
}
