package co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.GenerarReporteAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.entity.ReporteAsistenciaEntity;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.mapper.ReporteAsistenciaRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaQueryPort;
import java.util.List; import java.util.Objects; import java.util.UUID;

public final class GenerarReporteAsistenciaUseCaseImpl implements GenerarReporteAsistenciaUseCase {
    private final ReporteAsistenciaQueryPort queryPort;
    public GenerarReporteAsistenciaUseCaseImpl(final ReporteAsistenciaQueryPort queryPort) { this.queryPort = Objects.requireNonNull(queryPort); }
    @Override public List<ReporteAsistenciaEntity> execute(final UUID grupoId) {
        return queryPort.consultarReporteAsistenciaGrupo(grupoId).stream()
                .map(ReporteAsistenciaRepositoryMapper::toEntity)
                .toList();
    }
}
