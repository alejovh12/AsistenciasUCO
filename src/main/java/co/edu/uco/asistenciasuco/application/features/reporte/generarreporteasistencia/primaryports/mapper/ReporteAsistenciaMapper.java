package co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.dto.ReporteAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.entity.ReporteAsistenciaEntity;

public final class ReporteAsistenciaMapper {

    private ReporteAsistenciaMapper() {
    }

    public static ReporteAsistenciaDTO toDTO(final ReporteAsistenciaEntity entity) {
        return new ReporteAsistenciaDTO(
                entity.codigoGrupo(),
                entity.nombreGrupo(),
                entity.numeroSesion(),
                entity.nombreSesion(),
                entity.fechaHoraInicio(),
                entity.fechaHoraFin(),
                entity.documentoEstudiante(),
                entity.nombreEstudiante(),
                entity.correoEstudiante(),
                entity.asistio(),
                entity.razonCausa()
        );
    }
}
