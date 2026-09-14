package co.edu.uco.asistenciasuco.application.features.admin.consultarareas.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.primaryports.ConsultarAreasInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.primaryports.dto.AreaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.primaryports.mapper.ConsultarAreasMapper;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.usecase.ConsultarAreasUseCase;

import java.util.List;
import java.util.Objects;

public final class ConsultarAreasInteractor implements ConsultarAreasInputPort {
    private final ConsultarAreasUseCase useCase;
    public ConsultarAreasInteractor(final ConsultarAreasUseCase useCase) { this.useCase = Objects.requireNonNull(useCase); }
    @Override public List<AreaDTO> execute() { return useCase.execute().stream().map(ConsultarAreasMapper::toDTO).toList(); }
}
