package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** CULM-001 B.1 (HU047): cuerpo de {@code PATCH /api/v1/docente/sesiones/{sesionId}/cancelar}. */
public final class CancelarSesionRequest {

    @NotBlank(message = "El motivo de cancelacion es obligatorio.")
    @Size(max = 500, message = "El motivo de cancelacion no puede superar 500 caracteres.")
    private String motivo;

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(final String motivo) {
        this.motivo = motivo;
    }
}
