package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** CULM-001 C.1: cuerpo de los PATCH de revisiones de cancelacion ({@code decision}, {@code motivo}). Campos extra -> 400 (Jackson estricto). */
public final class DecisionRevisionCancelacionRequest {

    @NotBlank(message = "La decision es obligatoria.")
    private String decision;
    @NotNull(message = "El motivo es obligatorio.")
    private String motivo;

    public String getDecision() {
        return decision;
    }

    public void setDecision(final String decision) {
        this.decision = decision;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(final String motivo) {
        this.motivo = motivo;
    }
}
