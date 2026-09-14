package co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.primaryports.ConsultarParametrosInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.primaryports.dto.ParametroDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.primaryports.mapper.ConsultarParametrosMapper;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.usecase.ConsultarParametrosUseCase;

import java.util.List;
import java.util.Objects;

public final class ConsultarParametrosInteractor implements ConsultarParametrosInputPort {
    private final ConsultarParametrosUseCase useCase;
    public ConsultarParametrosInteractor(final ConsultarParametrosUseCase useCase) { this.useCase = Objects.requireNonNull(useCase); }
    @Override public List<ParametroDTO> execute() {
        return useCase.execute().stream().map(ConsultarParametrosMapper::toDTO).toList();
    }
}
