package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request;

import jakarta.validation.constraints.Size;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.SinEtiquetasHtml;
public final class ResolverReclamoRequest {

    private String accion;
    @SinEtiquetasHtml
    @Size(max = 500, message = "La respuesta del docente no puede superar 500 caracteres.")
    private String respuestaDocente;
    @SinEtiquetasHtml
    @Size(max = 500, message = "La respuesta del docente no puede superar 500 caracteres.")
    private String respuesta;
    /** PC (puntos de control): PRESENTE o JUSTIFICADA; opcional y solo con APROBADA (por defecto JUSTIFICADA). */
    private String resultado;

    public String getResultado() {
        return resultado;
    }

    public void setResultado(final String resultado) {
        this.resultado = resultado;
    }

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
