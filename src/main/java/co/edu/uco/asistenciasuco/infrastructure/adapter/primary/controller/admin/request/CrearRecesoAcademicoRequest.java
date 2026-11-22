package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.TextoSeguro;

import java.time.LocalDate;

/**
 * Cuerpo de {@code POST /api/v1/admin/periodos-academicos/{idPeriodo}/recesos} (DEC-081): un dia por registro. La fecha es
 * {@code yyyy-MM-dd}; que este dentro del periodo, sea futura y no festivo, y la longitud del motivo (REC_008) los decide la DB.
 */
public final class CrearRecesoAcademicoRequest {
    private LocalDate fecha;
    @TextoSeguro
    private String motivo;

    public LocalDate getFecha() { return fecha; }
    public void setFecha(final LocalDate fecha) { this.fecha = fecha; }
    public String getMotivo() { return motivo; }
    public void setMotivo(final String motivo) { this.motivo = motivo; }
}