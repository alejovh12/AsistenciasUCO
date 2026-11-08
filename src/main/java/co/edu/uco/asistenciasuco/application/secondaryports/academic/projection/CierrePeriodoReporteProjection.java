package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Proyeccion de reporte de cierre masivo de periodo (F-ADM-006).
 */
public record CierrePeriodoReporteProjection(
        UUID id,
        String periodoCodigo,
        OffsetDateTime fechaEjecucion,
        int totalEstudiantesProcesados,
        int totalMateriasAfectadas,
        int totalAprobadosAsistencia,
        int totalReprobadosFallas,
        String estado,
        String ejecutadoPor
) {
}
