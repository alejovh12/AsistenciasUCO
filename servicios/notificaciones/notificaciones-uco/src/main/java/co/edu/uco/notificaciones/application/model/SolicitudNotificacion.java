package co.edu.uco.notificaciones.application.model;

import co.edu.uco.notificaciones.domain.Destinatario;

import java.util.List;
import java.util.Map;

/** Pedido de una aplicacion cliente para enviar un aviso. */
public record SolicitudNotificacion(
        String aplicacion,
        String idempotencyKey,
        String tipo,
        String idioma,
        String asunto,
        String mensaje,
        List<Destinatario> destinatarios,
        Map<String, String> datos,
        String idCorrelacion,
        String traceparent
) {
}
