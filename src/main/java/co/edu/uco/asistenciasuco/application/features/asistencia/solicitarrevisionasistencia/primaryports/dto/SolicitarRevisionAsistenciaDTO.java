package co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.dto;

import java.util.UUID;

/**
 * DTO de entrada para solicitar la revision de una asistencia.
 */
public final class SolicitarRevisionAsistenciaDTO {

    private UUID sesion;
    private String categoria;
    private String justificacion;
    private String soporteNombre;
    private String soporteUrl;
    private UUID usuario;

    public SolicitarRevisionAsistenciaDTO() {
        super();
    }

    public SolicitarRevisionAsistenciaDTO(
            final UUID sesion,
            final String categoria,
            final String justificacion,
            final String soporteNombre,
            final String soporteUrl,
            final UUID usuario
    ) {
        setSesion(sesion);
        setCategoria(categoria);
        setJustificacion(justificacion);
        setSoporteNombre(soporteNombre);
        setSoporteUrl(soporteUrl);
        setUsuario(usuario);
    }

    public UUID getSesion() {
        return sesion;
    }

    public void setSesion(final UUID sesion) {
        this.sesion = sesion;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(final String categoria) {
        this.categoria = categoria;
    }

    public String getJustificacion() {
        return justificacion;
    }

    public void setJustificacion(final String justificacion) {
        this.justificacion = justificacion;
    }

    public String getSoporteNombre() {
        return soporteNombre;
    }

    public void setSoporteNombre(final String soporteNombre) {
        this.soporteNombre = soporteNombre;
    }

    public String getSoporteUrl() {
        return soporteUrl;
    }

    public void setSoporteUrl(final String soporteUrl) {
        this.soporteUrl = soporteUrl;
    }

    public UUID getUsuario() {
        return usuario;
    }

    public void setUsuario(final UUID usuario) {
        this.usuario = usuario;
    }

}
