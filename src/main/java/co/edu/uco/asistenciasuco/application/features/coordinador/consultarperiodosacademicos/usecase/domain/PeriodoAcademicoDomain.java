package co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.usecase.domain;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Modelo de resultado interno de la consulta de periodos academicos.
 */
public record PeriodoAcademicoDomain(
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
