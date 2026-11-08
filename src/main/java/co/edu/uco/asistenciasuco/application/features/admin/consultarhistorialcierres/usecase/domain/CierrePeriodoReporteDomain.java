package co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.usecase.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Modelo de dominio para reporte historico de cierre masivo de periodo (F-ADM-006).
 */
public record CierrePeriodoReporteDomain(
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
