package co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports;

import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.dto.ReporteAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.dto.GenerarReporteAsistenciaDTO;
import java.util.List; import java.util.UUID;

public interface GenerarReporteAsistenciaInputPort { List<ReporteAsistenciaDTO> execute(GenerarReporteAsistenciaDTO dto); }
