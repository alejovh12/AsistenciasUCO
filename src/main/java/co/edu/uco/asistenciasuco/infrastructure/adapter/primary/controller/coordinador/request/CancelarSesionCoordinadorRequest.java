package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request;

/** CULM-001 / DEC-064 (HU047): cuerpo de {@code POST /api/v1/coordinador/sesiones/{sesionId}/cancelacion}. */
public final class CancelarSesionCoordinadorRequest {

    private String codigoMotivo;
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
