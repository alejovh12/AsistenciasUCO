package co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.ErrorCode;
import co.edu.uco.asistenciasuco.application.exception.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;

import java.util.UUID;

/**
 * Dominio de la operacion cerrar sesion.
 */
public final class CerrarSesionDomain {

    private final UUID sesion;
    private final String observacionCierre;

    public CerrarSesionDomain(
            final UUID sesion,
            final String observacionCierre
    ) {
        validarSesion(sesion);
        this.observacionCierre = validarObservacionCierre(observacionCierre);

        this.sesion = sesion;
    }

    private void validarSesion(final UUID sesion) {
        if (ObjectHelper.isNull(sesion)) {
            throw new ValidationException(ErrorCode.ERR_SESION_REQUERIDA);
        }
    }

    private String validarObservacionCierre(final String observacionCierre) {
        final String observacionNormalizada = TextHelper.trim(observacionCierre);

        if (TextHelper.isNullOrBlank(observacionNormalizada)) {
            throw new ValidationException(ErrorCode.ERR_OBSERVACION_CIERRE_REQUERIDA);
        }

        if (!TextHelper.hasLengthBetween(observacionNormalizada, 10, 250)) {
            throw new ValidationException(ErrorCode.ERR_OBSERVACION_CIERRE_LONGITUD_INVALIDA);
        }

        return observacionNormalizada;
    }

    public UUID getSesion() {
        return sesion;
    }

    public String getObservacionCierre() {
        return observacionCierre;
    }

}
