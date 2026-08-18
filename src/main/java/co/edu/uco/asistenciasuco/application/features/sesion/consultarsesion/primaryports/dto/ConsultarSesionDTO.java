package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto;

import java.util.UUID;

/**
 * DTO de entrada para consultar una sesion.
 */
public final class ConsultarSesionDTO {

    private UUID sesion;

    public ConsultarSesionDTO() {
        super();
    }

    public ConsultarSesionDTO(final UUID sesion) {
        setSesion(sesion);
    }

    public UUID getSesion() {
        return sesion;
    }

    public void setSesion(final UUID sesion) {
        this.sesion = sesion;
    }

}
