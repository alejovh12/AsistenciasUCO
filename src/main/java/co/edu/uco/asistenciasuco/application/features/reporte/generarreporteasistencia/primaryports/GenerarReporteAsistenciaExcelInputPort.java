package co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports;

import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.dto.GenerarReporteAsistenciaDTO;

/** Genera el reporte de asistencia del grupo ya serializado como libro de Excel (.xlsx). */
public interface GenerarReporteAsistenciaExcelInputPort {

    byte[] execute(GenerarReporteAsistenciaDTO dto);
}
