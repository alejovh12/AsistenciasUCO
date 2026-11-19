package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request;

/**
 * HU074: cuerpo opcional de {@code PATCH /api/v1/coordinador/asignaturas/{id}/estado}. Sin cuerpo o con {@code estaActiva}
 * en {@code null} se alterna el estado actual; con {@code true}/{@code false} la asignatura queda en ese estado (si ya lo
 * estaba, la DB confirma sin cambio).
 */
public final class CambiarEstadoAsignaturaRequest {

    private Boolean estaActiva;

    public Boolean getEstaActiva() {
        return estaActiva;
    }

    public void setEstaActiva(final Boolean estaActiva) {
        this.estaActiva = estaActiva;
    }
}
