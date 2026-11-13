package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request;

import jakarta.validation.constraints.Size;

/** DEC-065 (2): cuerpo de {@code POST /api/v1/coordinador/sesiones/{sesionId}/aprobacion}; la observacion es opcional. */
public final class AprobarSolicitudSesionRequest {

    @Size(max = 500, message = "La observacion no puede superar 500 caracteres.")
    private String observacion;

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(final String observacion) {
        this.observacion = observacion;
    }
}
