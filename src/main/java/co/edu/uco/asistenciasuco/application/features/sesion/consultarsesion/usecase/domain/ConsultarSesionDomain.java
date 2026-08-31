package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.domain;


import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.UUID;

/**
 * Dominio de la operacion consultar sesion.
 */
public final class ConsultarSesionDomain {

    private final UUID sesion;

    public ConsultarSesionDomain(final UUID sesion) {
        validarSesion(sesion);

        this.sesion = sesion;
    }

    private void validarSesion(final UUID sesion) {
        if (ObjectHelper.isNull(sesion)) {
            throw new ValidationException(SesionErrorCode.ERR_SESION_REQUERIDA);
        }
    }

    public UUID getSesion() {
        return sesion;
    }

}
