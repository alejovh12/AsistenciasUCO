package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request;

import jakarta.validation.constraints.NotNull;

/** Cuerpo de {@code PATCH /api/v1/coordinador/docentes/{idDocente}/estado}. */
public final class CambiarEstadoDocenteProgramaRequest {
    @NotNull(message = "El estado (estaActivo) es obligatorio.")
    private Boolean estaActivo;

    public Boolean getEstaActivo() { return estaActivo; }
    public void setEstaActivo(final Boolean estaActivo) { this.estaActivo = estaActivo; }
}
