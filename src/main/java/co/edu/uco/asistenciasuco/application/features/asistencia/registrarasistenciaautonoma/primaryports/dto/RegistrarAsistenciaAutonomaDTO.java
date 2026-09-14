package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.primaryports.dto;

import java.util.UUID;

public final class RegistrarAsistenciaAutonomaDTO {

    private UUID sesion;
    private String codigoVerificacion;
    private UUID usuario;

    public RegistrarAsistenciaAutonomaDTO() {
        super();
    }

    public RegistrarAsistenciaAutonomaDTO(final UUID sesion, final String codigoVerificacion, final UUID usuario) {
        setSesion(sesion);
        setCodigoVerificacion(codigoVerificacion);
        setUsuario(usuario);
    }

    public UUID getSesion() {
        return sesion;
    }

    public void setSesion(final UUID sesion) {
        this.sesion = sesion;
    }

    public String getCodigoVerificacion() {
        return codigoVerificacion;
    }

    public void setCodigoVerificacion(final String codigoVerificacion) {
        this.codigoVerificacion = codigoVerificacion;
    }

    public UUID getUsuario() {
        return usuario;
    }

    public void setUsuario(final UUID usuario) {
        this.usuario = usuario;
    }
}
