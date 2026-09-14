package co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.entity.ReporteAsistenciaEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaRow;

public final class ReporteAsistenciaRepositoryMapper {

    private ReporteAsistenciaRepositoryMapper() {
    }

    public static ReporteAsistenciaEntity toEntity(final ReporteAsistenciaRow row) {
        return new ReporteAsistenciaEntity(
                row.codigoGrupo(),
                row.nombreGrupo(),
                row.numeroSesion(),
                row.nombreSesion(),
                row.fechaHoraInicio(),
                row.fechaHoraFin(),
                row.documentoEstudiante(),
                row.nombreEstudiante(),
                row.correoEstudiante(),
                row.asistio(),
                row.razonCausa()
        );
    }
}
