package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.CrearCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.dto.CrearCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.dto.CrearCoordinadorResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.mapper.CrearCoordinadorMapper;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.CrearCoordinadorUseCase;
import java.util.Objects;

public final class CrearCoordinadorInteractor implements CrearCoordinadorInputPort {

    private final CrearCoordinadorUseCase useCase;

    public CrearCoordinadorInteractor(final CrearCoordinadorUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "CrearCoordinadorUseCase es obligatorio.");
    }

    @Override
    public CrearCoordinadorResultadoDTO execute(final CrearCoordinadorDTO dto) {
        return CrearCoordinadorMapper.toResultadoDto(useCase.execute(CrearCoordinadorMapper.toDomain(dto)));
    }
}
