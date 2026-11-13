package co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;

import java.util.UUID;

/** Criterios del detalle: solicitud y usuario autenticado, ambos obligatorios. */
public record ConsultarDetalleReclamoDocenteDomain(UUID idSolicitud, UUID actorUsuarioId) {

    public static ConsultarDetalleReclamoDocenteDomain of(final UUID idSolicitud, final UUID actorUsuarioId) {
        if (actorUsuarioId == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_008);
        }
        if (idSolicitud == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_051);
        }
        return new ConsultarDetalleReclamoDocenteDomain(idSolicitud, actorUsuarioId);
    }
}
