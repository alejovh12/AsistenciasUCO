package co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.dto;

import java.util.UUID;

public final class ResolverSolicitudRevisionAsistenciaDTO {

    private UUID solicitud;
    private String accion;
    private String respuestaDocente;
    private UUID usuario;

    public ResolverSolicitudRevisionAsistenciaDTO() {
        super();
    }

    public ResolverSolicitudRevisionAsistenciaDTO(
            final UUID solicitud,
            final String accion,
            final String respuestaDocente,
            final UUID usuario
    ) {
        setSolicitud(solicitud);
        setAccion(accion);
        setRespuestaDocente(respuestaDocente);
        setUsuario(usuario);
    }

    public UUID getSolicitud() {
        return solicitud;
    }

    public void setSolicitud(final UUID solicitud) {
        this.solicitud = solicitud;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(final String accion) {
        this.accion = accion;
    }

    public String getRespuestaDocente() {
        return respuestaDocente;
    }

    public void setRespuestaDocente(final String respuestaDocente) {
        this.respuestaDocente = respuestaDocente;
    }

    public UUID getUsuario() {
        return usuario;
    }

    public void setUsuario(final UUID usuario) {
        this.usuario = usuario;
    }
}
