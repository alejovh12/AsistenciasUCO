package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox;

import java.time.Instant;
import java.util.UUID;

/**
 * Fila devuelta por la reserva de notificaciones en el outbox (CONTRACT §2.2).
 */
public record NotificacionReservada(
        UUID id,
        String tipo,
        String asunto,
        String mensaje,
        String destinatariosJson,
        String datosJson,
        int intentos,
        Instant creadoEn,
        UUID idCorrelacion,
        String traceId,
        String spanId
) {
    /**
     * Constructor de compatibilidad para fixtures que pasaban maxIntentos.
     */
    public NotificacionReservada(
            final UUID id,
            final String tipo,
            final String asunto,
            final String mensaje,
            final String destinatariosJson,
            final String datosJson,
            final int intentos,
            final int maxIntentos,
            final Instant creadoEn,
            final String traceId,
            final String spanId
    ) {
        this(id, tipo, asunto, mensaje, destinatariosJson, datosJson, intentos, creadoEn, null, traceId, spanId);
    }
}
