package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request;

/** DEC-065 (2): cuerpo de {@code POST /api/v1/coordinador/sesiones/{sesionId}/aprobacion}; la observacion es opcional. */
public final class AprobarSolicitudSesionRequest {

    private String observacion;

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(final String observacion) {
        this.observacion = observacion;
    }
}
