package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request;

public final class ResolverReclamoRequest {

    private String accion;
    private String respuestaDocente;
    private String respuesta;

    public String getAccion() {
        return accion;
    }

    public void setAccion(final String accion) {
        this.accion = accion;
    }

    public String getRespuestaDocente() {
        return respuestaDocente == null ? respuesta : respuestaDocente;
    }

    public void setRespuestaDocente(final String respuestaDocente) {
        this.respuestaDocente = respuestaDocente;
    }

    public void setRespuesta(final String respuesta) {
        this.respuesta = respuesta;
    }
}
