package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.request;

/** CULM-001 C.1: cuerpo de los PATCH de revisiones de cancelacion ({@code decision}, {@code motivo}). Campos extra -> 400 (Jackson estricto). */
public final class DecisionRevisionCancelacionRequest {

    private String decision;
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
