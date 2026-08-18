package co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.usecase.domain;

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
            throw new IllegalArgumentException("La sesion es obligatoria.");
        }
    }

    private String validarObservacionCierre(final String observacionCierre) {
        final String observacionNormalizada = TextHelper.trim(observacionCierre);

        if (TextHelper.isNullOrBlank(observacionNormalizada)) {
            throw new IllegalArgumentException("La observacion de cierre es obligatoria.");
        }

        if (!TextHelper.hasLengthBetween(observacionNormalizada, 10, 250)) {
            throw new IllegalArgumentException("La observacion de cierre debe tener entre 10 y 250 caracteres.");
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
