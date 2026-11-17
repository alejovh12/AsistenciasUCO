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
        Integer anio,
        String estado
) {

    public PeriodoAcademicoDomain(
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
