package co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.primaryports.ActualizarCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.primaryports.dto.ActualizarCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.primaryports.mapper.ActualizarCoordinadorMapper;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.usecase.ActualizarCoordinadorUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.common.dto.ResultadoGestionCoordinadorDTO;

import java.util.Objects;

public final class ActualizarCoordinadorInteractor implements ActualizarCoordinadorInputPort {

    private final ActualizarCoordinadorUseCase useCase;

    public ActualizarCoordinadorInteractor(final ActualizarCoordinadorUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "ActualizarCoordinadorUseCase es obligatorio.");
    }

    @Override
    public ResultadoGestionCoordinadorDTO execute(final ActualizarCoordinadorDTO dto) {
        return ActualizarCoordinadorMapper.toDTO(useCase.execute(ActualizarCoordinadorMapper.toDomain(dto)));
    }
}
