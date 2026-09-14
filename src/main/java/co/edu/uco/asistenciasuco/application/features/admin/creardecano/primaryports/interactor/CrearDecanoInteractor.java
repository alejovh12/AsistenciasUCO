package co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.CrearDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.dto.CrearDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.mapper.CrearDecanoMapper;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.CrearDecanoUseCase;

import java.util.Objects;

public final class CrearDecanoInteractor implements CrearDecanoInputPort {
    private final CrearDecanoUseCase useCase;
    public CrearDecanoInteractor(final CrearDecanoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "CrearDecanoUseCase es obligatorio.");
    }
    @Override
    public void execute(final CrearDecanoDTO dto) {
        useCase.execute(CrearDecanoMapper.toDomain(dto));
    }
}
