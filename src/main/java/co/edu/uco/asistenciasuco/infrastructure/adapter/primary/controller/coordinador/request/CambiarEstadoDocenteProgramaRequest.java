package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request;

/** Cuerpo de {@code PATCH /api/v1/coordinador/docentes/{idDocente}/estado}. */
public final class CambiarEstadoDocenteProgramaRequest {
    private Boolean estaActivo;

    public Boolean getEstaActivo() { return estaActivo; }
    public void setEstaActivo(final Boolean estaActivo) { this.estaActivo = estaActivo; }
}
