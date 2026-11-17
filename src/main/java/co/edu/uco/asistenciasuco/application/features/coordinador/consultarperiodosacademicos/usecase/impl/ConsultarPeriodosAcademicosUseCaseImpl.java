package co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.usecase.ConsultarPeriodosAcademicosUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.usecase.domain.PeriodoAcademicoDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.usecase.mapper.ConsultarPeriodosAcademicosRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PeriodoAcademicoQueryPort;
import java.util.List; import java.util.Objects;

public final class ConsultarPeriodosAcademicosUseCaseImpl implements ConsultarPeriodosAcademicosUseCase {
    private final PeriodoAcademicoQueryPort queryPort;
    public ConsultarPeriodosAcademicosUseCaseImpl(final PeriodoAcademicoQueryPort queryPort) { this.queryPort = Objects.requireNonNull(queryPort); }
    @Override public List<PeriodoAcademicoDomain> execute() {
        return queryPort.consultarPeriodosAcademicos().stream().map(ConsultarPeriodosAcademicosRepositoryMapper::toDomain).toList();
    }

    @Override public List<PeriodoAcademicoDomain> execute(final String estado) {
        if (estado == null || estado.isBlank()) {
            return execute();
        }
        return queryPort.consultarPeriodosAcademicosPorEstado(estado.trim().toUpperCase(java.util.Locale.ROOT)).stream()
                .map(ConsultarPeriodosAcademicosRepositoryMapper::toDomain).toList();
    }
}
