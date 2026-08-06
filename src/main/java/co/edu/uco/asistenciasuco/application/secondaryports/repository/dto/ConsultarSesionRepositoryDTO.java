package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

/**
 * DTO del puerto secundario para consultar una sesion.
 */
public final class ConsultarSesionRepositoryDTO {

    private UUID sesion;
    private UUID idCorrelacion;

    public ConsultarSesionRepositoryDTO() {
        super();
    }

    public ConsultarSesionRepositoryDTO(final UUID sesion, final UUID idCorrelacion) {
        setSesion(sesion);
        setIdCorrelacion(idCorrelacion);
    }

    public UUID getSesion() {
        return sesion;
    }

    public void setSesion(final UUID sesion) {
        this.sesion = sesion;
    }

    public UUID getIdCorrelacion() {
        return idCorrelacion;
    }

    public void setIdCorrelacion(final UUID idCorrelacion) {
        this.idCorrelacion = idCorrelacion;
    }
}
