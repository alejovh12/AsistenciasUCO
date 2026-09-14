package co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.PeriodoAcademicoDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.primaryports.ConsultarPeriodosAcademicosInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.primaryports.mapper.ConsultarPeriodosAcademicosMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.usecase.ConsultarPeriodosAcademicosUseCase;
import java.util.List; import java.util.Objects;

public final class ConsultarPeriodosAcademicosInteractor implements ConsultarPeriodosAcademicosInputPort {
    private final ConsultarPeriodosAcademicosUseCase useCase;
    public ConsultarPeriodosAcademicosInteractor(final ConsultarPeriodosAcademicosUseCase useCase) { this.useCase = Objects.requireNonNull(useCase); }
    @Override public List<PeriodoAcademicoDTO> execute() { return useCase.execute().stream().map(ConsultarPeriodosAcademicosMapper::toDTO).toList(); }
}
