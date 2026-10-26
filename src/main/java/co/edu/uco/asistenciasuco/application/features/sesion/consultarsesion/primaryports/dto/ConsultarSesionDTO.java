package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto;

import java.util.UUID;

/**
 * DTO de entrada para consultar una sesion.
 */
public final class ConsultarSesionDTO {

    private UUID sesion;
    private UUID usuarioEjecutor;

    public ConsultarSesionDTO() {
        super();
    }

    public ConsultarSesionDTO(final UUID sesion, final UUID usuarioEjecutor) {
        setSesion(sesion);
        setUsuarioEjecutor(usuarioEjecutor);
    }

    public UUID getSesion() {
        return sesion;
    }

    public void setSesion(final UUID sesion) {
        this.sesion = sesion;
    }

    public UUID getUsuarioEjecutor() {
        return usuarioEjecutor;
    }

    public void setUsuarioEjecutor(final UUID usuarioEjecutor) {
        this.usuarioEjecutor = usuarioEjecutor;
    }

}
