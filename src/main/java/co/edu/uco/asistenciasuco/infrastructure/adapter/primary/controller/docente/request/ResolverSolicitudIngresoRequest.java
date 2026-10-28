package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request;

/** HU053/HU054: cuerpo de {@code PATCH /api/v1/docente/solicitudes-ingreso/{id}}. */
public final class ResolverSolicitudIngresoRequest {

    private String accion;
    private String motivo;

    public String getAccion() {
        return accion;
    }

    public void setAccion(final String accion) {
        this.accion = accion;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(final String motivo) {
        this.motivo = motivo;
    }
}
