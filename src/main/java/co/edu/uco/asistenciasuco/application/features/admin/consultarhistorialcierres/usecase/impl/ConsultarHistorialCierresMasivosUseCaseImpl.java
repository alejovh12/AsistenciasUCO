package co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.usecase.ConsultarHistorialCierresMasivosUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.usecase.domain.CierrePeriodoReporteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.HistorialCierrePeriodoQueryPort;

import java.util.List;
import java.util.Objects;

/**
 * Implementacion del caso de uso para consultar el historial de reportes de cierre masivo (F-ADM-006).
 */
public final class ConsultarHistorialCierresMasivosUseCaseImpl implements ConsultarHistorialCierresMasivosUseCase {

    private final HistorialCierrePeriodoQueryPort queryPort;

    public ConsultarHistorialCierresMasivosUseCaseImpl(final HistorialCierrePeriodoQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort, "HistorialCierrePeriodoQueryPort es obligatorio.");
    }

    @Override
    public List<CierrePeriodoReporteDomain> execute() {
        return queryPort.consultarHistorialCierres().stream()
                .map(p -> new CierrePeriodoReporteDomain(
                        p.id(),
                        p.periodoCodigo(),
                        p.fechaEjecucion(),
                        p.totalEstudiantesProcesados(),
                        p.totalMateriasAfectadas(),
                        p.totalAprobadosAsistencia(),
                        p.totalReprobadosFallas(),
                        p.estado(),
                        p.ejecutadoPor()
                ))
                .toList();
    }
}
