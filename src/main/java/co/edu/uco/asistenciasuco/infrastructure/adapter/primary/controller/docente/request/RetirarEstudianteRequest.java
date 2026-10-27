package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request;

/** HU051: cuerpo de {@code POST /api/v1/docente/grupos/{grupoId}/estudiantes/{estudianteId}/retiro}. */
public final class RetirarEstudianteRequest {

    private String motivo;

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(final String motivo) {
        this.motivo = motivo;
    }
}
