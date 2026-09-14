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
        Integer anio
) {
}
