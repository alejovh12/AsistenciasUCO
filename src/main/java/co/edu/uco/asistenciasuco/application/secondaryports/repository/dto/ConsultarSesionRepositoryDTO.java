package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

/**
 * DTO del puerto secundario para consultar una sesion.
 */
public final class ConsultarSesionRepositoryDTO {

    private UUID sesion;

    public ConsultarSesionRepositoryDTO() {
        super();
    }

    public ConsultarSesionRepositoryDTO(final UUID sesion) {
        setSesion(sesion);
    }

    public UUID getSesion() {
        return sesion;
    }

    public void setSesion(final UUID sesion) {
        this.sesion = sesion;
    }

}
