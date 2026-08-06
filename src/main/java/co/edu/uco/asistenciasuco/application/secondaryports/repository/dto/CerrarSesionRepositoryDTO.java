package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

/**
 * DTO del puerto secundario para cerrar una sesion.
 */
public final class CerrarSesionRepositoryDTO {

    private UUID sesion;
    private String observacionCierre;
    private UUID idCorrelacion;

    public CerrarSesionRepositoryDTO() {
        super();
    }

    public CerrarSesionRepositoryDTO(
            final UUID sesion,
            final String observacionCierre,
            final UUID idCorrelacion
    ) {
        setSesion(sesion);
        setObservacionCierre(observacionCierre);
        setIdCorrelacion(idCorrelacion);
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

    public UUID getIdCorrelacion() {
        return idCorrelacion;
    }

    public void setIdCorrelacion(final UUID idCorrelacion) {
        this.idCorrelacion = idCorrelacion;
    }
}
