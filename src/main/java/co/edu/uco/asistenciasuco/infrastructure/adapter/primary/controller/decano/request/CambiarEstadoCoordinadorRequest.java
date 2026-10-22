package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.request;

/**
 * Cuerpo de {@code PATCH /api/v1/decano/coordinadores/{id}/estado} (HU122). El estado destino es explicito:
 * nunca se asume un toggle.
 */
public final class CambiarEstadoCoordinadorRequest {
    private Boolean estaActivo;

    public Boolean getEstaActivo() { return estaActivo; }
    public void setEstaActivo(final Boolean estaActivo) { this.estaActivo = estaActivo; }
}
