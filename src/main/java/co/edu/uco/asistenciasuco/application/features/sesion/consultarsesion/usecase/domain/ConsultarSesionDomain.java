package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.domain;


import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.UUID;

/**
 * Dominio de la operacion consultar sesion.
 */
public final class ConsultarSesionDomain {

    private final UUID sesion;
    private final UUID usuarioEjecutor;

    public ConsultarSesionDomain(final UUID sesion, final UUID usuarioEjecutor) {
        validarSesion(sesion);
        if (ObjectHelper.isNull(usuarioEjecutor)) {
            throw new ValidationException(SesionErrorCode.ERR_DOCENTE_REQUERIDO);
        }

        this.sesion = sesion;
        this.usuarioEjecutor = usuarioEjecutor;
    }

    private void validarSesion(final UUID sesion) {
        if (ObjectHelper.isNull(sesion)) {
            throw new ValidationException(SesionErrorCode.ERR_SESION_REQUERIDA);
        }
    }

    public UUID getSesion() {
        return sesion;
    }

    public UUID getUsuarioEjecutor() {
        return usuarioEjecutor;
    }

}
