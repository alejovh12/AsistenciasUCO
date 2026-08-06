package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.domain;

import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.UUID;

/**
 * Dominio de la operacion consultar sesion.
 */
public final class ConsultarSesionDomain {

    private final UUID sesion;
    private final UUID idCorrelacion;

    public ConsultarSesionDomain(final UUID sesion, final UUID idCorrelacion) {
        validarSesion(sesion);
        validarIdCorrelacion(idCorrelacion);

        this.sesion = sesion;
        this.idCorrelacion = idCorrelacion;
    }

    private void validarSesion(final UUID sesion) {
        if (ObjectHelper.isNull(sesion)) {
            throw new IllegalArgumentException("La sesion es obligatoria.");
        }
    }

    private void validarIdCorrelacion(final UUID idCorrelacion) {
        if (ObjectHelper.isNull(idCorrelacion)) {
            throw new IllegalArgumentException("El id de correlacion es obligatorio.");
        }
    }

    public UUID getSesion() {
        return sesion;
    }

    public UUID getIdCorrelacion() {
        return idCorrelacion;
    }
}
