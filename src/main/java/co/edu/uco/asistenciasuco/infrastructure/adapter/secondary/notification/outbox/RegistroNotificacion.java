package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox;

import java.util.UUID;

/**
 * Parametros para el registro de una notificacion en el outbox.
 */
public record RegistroNotificacion(
        UUID id,
        String tipo,
        String asunto,
        String mensaje,
        String destinatariosJson,
        String datosJson,
        UUID idCorrelacion,
        String traceId,
        String spanId
) {
}
