package co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase;

import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.entity.ReporteAsistenciaEntity;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.domain.GenerarReporteAsistenciaDomain;
import java.util.List;

public interface GenerarReporteAsistenciaUseCase { List<ReporteAsistenciaEntity> execute(GenerarReporteAsistenciaDomain domain); }
