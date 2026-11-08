package co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.primaryports.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * F-ADM-006: DTO de reporte historico de cierre masivo de periodo academico.
 */
public record CierrePeriodoReporteDTO(
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
