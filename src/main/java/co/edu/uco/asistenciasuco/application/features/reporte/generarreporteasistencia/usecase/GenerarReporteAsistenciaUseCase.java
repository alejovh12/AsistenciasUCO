package co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase;

import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.entity.ReporteAsistenciaEntity;
import java.util.List; import java.util.UUID;

public interface GenerarReporteAsistenciaUseCase { List<ReporteAsistenciaEntity> execute(UUID grupoId); }
