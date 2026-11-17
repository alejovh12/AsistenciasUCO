package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.LocalDate;
import java.util.UUID;

public record PeriodoAcademicoProjection(
        UUID id,
        UUID idInstitucion,
        String nombreInstitucion,
        String nombre,
        String codigo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Integer anio,
        String estado
) {

    public PeriodoAcademicoProjection(
            final UUID id,
            final UUID idInstitucion,
            final String nombreInstitucion,
            final String nombre,
            final String codigo,
            final LocalDate fechaInicio,
            final LocalDate fechaFin,
            final Integer anio
    ) {
        this(id, idInstitucion, nombreInstitucion, nombre, codigo, fechaInicio, fechaFin, anio, null);
    }
}
