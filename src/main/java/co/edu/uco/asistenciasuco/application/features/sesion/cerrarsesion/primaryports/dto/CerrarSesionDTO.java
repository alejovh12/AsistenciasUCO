package co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.dto;

import java.util.UUID;

/**
 * DTO de entrada para cerrar una sesion.
 */
public final class CerrarSesionDTO {

    private UUID sesion;
    private String observacionCierre;

    public CerrarSesionDTO() {
        super();
    }

    public CerrarSesionDTO(
            final UUID sesion,
            final String observacionCierre
    ) {
        setSesion(sesion);
        setObservacionCierre(observacionCierre);
    }

    public UUID getSesion() {
        return sesion;
    }

    public void setSesion(final UUID sesion) {
        this.sesion = sesion;
    }

    public String getObservacionCierre() {
        return observacionCierre;
    }

    public void setObservacionCierre(final String observacionCierre) {
        this.observacionCierre = observacionCierre;
    }

}
