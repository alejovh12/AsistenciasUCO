package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

/**
 * DTO del puerto secundario para cerrar una sesion.
 */
public final class CerrarSesionRepositoryDTO {

    private UUID sesion;
    private String observacionCierre;

    public CerrarSesionRepositoryDTO() {
        super();
    }

    public CerrarSesionRepositoryDTO(
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
