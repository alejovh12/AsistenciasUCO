package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request;

import java.time.LocalDate;

/** Cuerpo de {@code POST/PUT /api/v1/admin/periodos-academicos}. Codigo y anio los deriva la DB. */
public final class GuardarPeriodoAcademicoRequest {
    private String nombre;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    public String getNombre() { return nombre; }
    public void setNombre(final String nombre) { this.nombre = nombre; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(final LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(final LocalDate fechaFin) { this.fechaFin = fechaFin; }
}
