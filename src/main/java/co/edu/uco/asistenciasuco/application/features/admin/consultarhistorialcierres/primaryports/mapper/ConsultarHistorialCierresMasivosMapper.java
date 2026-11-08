package co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.primaryports.dto.CierrePeriodoReporteDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.usecase.domain.CierrePeriodoReporteDomain;

/**
 * Mapper para convertir el modelo de dominio a DTO de salida (F-ADM-006).
 */
public final class ConsultarHistorialCierresMasivosMapper {

    private ConsultarHistorialCierresMasivosMapper() {
    }

    public static CierrePeriodoReporteDTO toDTO(final CierrePeriodoReporteDomain domain) {
        if (domain == null) {
            return null;
        }
        return new CierrePeriodoReporteDTO(
                domain.id(),
                domain.periodoCodigo(),
                domain.fechaEjecucion(),
                domain.totalEstudiantesProcesados(),
                domain.totalMateriasAfectadas(),
                domain.totalAprobadosAsistencia(),
                domain.totalReprobadosFallas(),
                domain.estado(),
                domain.ejecutadoPor()
        );
    }
}
