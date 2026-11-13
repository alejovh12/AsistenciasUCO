package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request;

import jakarta.validation.constraints.Size;

/** HU051: cuerpo de {@code POST /api/v1/docente/grupos/{grupoId}/estudiantes/{estudianteId}/retiro}. */
public final class RetirarEstudianteRequest {

    // Sin @NotBlank a proposito: el motivo ausente lo rechaza Application (HU051, RetirarEstudianteGrupoDomain).
    @Size(max = 500, message = "El motivo del retiro es obligatorio y no puede superar 500 caracteres.")
    private String motivo;

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(final String motivo) {
        this.motivo = motivo;
    }
}
