package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** CULM-001 / DEC-064 (HU047): cuerpo de {@code POST /api/v1/coordinador/sesiones/{sesionId}/cancelacion}. */
public final class CancelarSesionCoordinadorRequest {

    @NotBlank(message = "El motivo de cancelacion es obligatorio.")
    @Size(max = 50, message = "El codigo del motivo no puede superar 50 caracteres.")
    private String codigoMotivo;
    @Size(max = 500, message = "La observacion no puede superar 500 caracteres.")
    private String observacion;

    public String getCodigoMotivo() {
        return codigoMotivo;
    }

    public void setCodigoMotivo(final String codigoMotivo) {
        this.codigoMotivo = codigoMotivo;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(final String observacion) {
        this.observacion = observacion;
    }
}
