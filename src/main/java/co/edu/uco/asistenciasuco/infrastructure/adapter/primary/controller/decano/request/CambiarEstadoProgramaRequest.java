package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.request;

/** Cuerpo del cambio de estado: el estado destino es explicito (idempotente). */
public final class CambiarEstadoProgramaRequest {
    private Boolean estaActivo;

    public Boolean getEstaActivo() { return estaActivo; }
    public void setEstaActivo(final Boolean estaActivo) { this.estaActivo = estaActivo; }
}
