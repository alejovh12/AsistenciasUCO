package co.edu.uco.notificaciones.infrastructure.adapter.in.rest.dto;

import co.edu.uco.notificaciones.domain.Notificacion;

import java.time.Instant;
import java.util.UUID;

/** Fila de la cola administrativa. No incluye correos ni datos: solo cuantos destinatarios tiene. */
public record NotificacionAdminResponse(
        UUID id,
        String aplicacion,
        String tipo,
        String asunto,
        String codigoEstado,
        String nombreEstado,
        boolean esTerminal,
        int cantidadDestinatarios,
        int intentos,
        Instant creadoEn,
        Instant proximoIntentoEn,
        Instant entregadoEn,
        String codigoError,
        String detalleError,
        String idCorrelacion
) {

    public static NotificacionAdminResponse desde(final Notificacion n) {
        return new NotificacionAdminResponse(n.id(), n.aplicacion(), n.tipo(), n.asunto() != null ? n.asunto() : n.tipo(),
                n.estado().name(), n.estado().nombre(), n.estado().esTerminal(), n.destinatarios().size(), n.intentos(),
                n.creadoEn(), n.proximoIntentoEn(), n.entregadoEn(), n.codigoError(), n.detalleError(), n.idCorrelacion());
    }
}
