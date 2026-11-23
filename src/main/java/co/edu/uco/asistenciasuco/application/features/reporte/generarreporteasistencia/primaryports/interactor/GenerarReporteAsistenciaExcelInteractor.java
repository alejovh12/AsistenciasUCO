package co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.GenerarReporteAsistenciaExcelInputPort;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.dto.GenerarReporteAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.mapper.ReporteAsistenciaMapper;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.GenerarReporteAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaRow;
import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteExcelPort;

import java.util.List;
import java.util.Objects;

public final class GenerarReporteAsistenciaExcelInteractor implements GenerarReporteAsistenciaExcelInputPort {

    private final GenerarReporteAsistenciaUseCase useCase;
    private final ReporteExcelPort reporteExcelPort;

    public GenerarReporteAsistenciaExcelInteractor(
            final GenerarReporteAsistenciaUseCase useCase,
            final ReporteExcelPort reporteExcelPort
    ) {
        this.useCase = Objects.requireNonNull(useCase, "GenerarReporteAsistenciaUseCase es obligatorio.");
        this.reporteExcelPort = Objects.requireNonNull(reporteExcelPort, "ReporteExcelPort es obligatorio.");
    }

    @Override
    public byte[] execute(final GenerarReporteAsistenciaDTO dto) {
        final List<ReporteAsistenciaRow> filas = useCase.execute(ReporteAsistenciaMapper.toDomain(dto)).stream()
                .map(entity -> new ReporteAsistenciaRow(entity.codigoGrupo(), entity.nombreGrupo(), entity.numeroSesion(),
                        entity.nombreSesion(), entity.fechaHoraInicio(), entity.fechaHoraFin(), entity.documentoEstudiante(),
                        entity.nombreEstudiante(), entity.correoEstudiante(), entity.asistio(), entity.razonCausa(),
                        entity.sesionCancelada()))
                .toList();
        return reporteExcelPort.generarReporteAsistencia(filas);
    }
}
