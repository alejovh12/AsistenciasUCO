package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class GuardarPlanEstudioRequest {
    @NotNull(message = "El INP del plan de estudio es obligatorio.")
    @Positive(message = "El INP del plan de estudio debe ser un entero positivo.")
    private Integer inp;
    public Integer getInp() { return inp; }
    public void setInp(final Integer inp) { this.inp = inp; }
}
