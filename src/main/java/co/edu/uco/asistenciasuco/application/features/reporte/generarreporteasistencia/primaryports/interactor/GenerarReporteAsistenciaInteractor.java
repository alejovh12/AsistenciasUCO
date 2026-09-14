package co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.GenerarReporteAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.dto.ReporteAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.mapper.ReporteAsistenciaMapper;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.GenerarReporteAsistenciaUseCase;
import java.util.List; import java.util.Objects; import java.util.UUID;

public final class GenerarReporteAsistenciaInteractor implements GenerarReporteAsistenciaInputPort {
    private final GenerarReporteAsistenciaUseCase useCase;
    public GenerarReporteAsistenciaInteractor(final GenerarReporteAsistenciaUseCase useCase) { this.useCase = Objects.requireNonNull(useCase); }
    @Override public List<ReporteAsistenciaDTO> execute(final UUID grupoId) {
        return useCase.execute(grupoId).stream().map(ReporteAsistenciaMapper::toDTO).toList();
    }
}
