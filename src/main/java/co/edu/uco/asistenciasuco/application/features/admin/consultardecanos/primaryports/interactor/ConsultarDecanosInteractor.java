package co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.ConsultarDecanosInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.dto.DecanoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.mapper.ConsultarDecanosMapper;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.usecase.ConsultarDecanosUseCase;

import java.util.List;
import java.util.Objects;

public final class ConsultarDecanosInteractor implements ConsultarDecanosInputPort {
    private final ConsultarDecanosUseCase useCase;
    public ConsultarDecanosInteractor(final ConsultarDecanosUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "ConsultarDecanosUseCase es obligatorio.");
    }
    @Override public List<DecanoDTO> execute() {
        return useCase.execute().stream().map(ConsultarDecanosMapper::toDTO).toList();
    }
}
