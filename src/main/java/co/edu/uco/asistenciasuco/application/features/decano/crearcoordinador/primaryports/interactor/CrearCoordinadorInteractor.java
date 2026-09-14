package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.CrearCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.dto.CrearCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.mapper.CrearCoordinadorMapper;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.CrearCoordinadorUseCase;
import java.util.Objects;

public final class CrearCoordinadorInteractor implements CrearCoordinadorInputPort {

    private final CrearCoordinadorUseCase useCase;

    public CrearCoordinadorInteractor(final CrearCoordinadorUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "CrearCoordinadorUseCase es obligatorio.");
    }

    @Override
    public void execute(final CrearCoordinadorDTO dto) {
        useCase.execute(CrearCoordinadorMapper.toDomain(dto));
    }
}
