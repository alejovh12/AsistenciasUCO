package co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.primaryports.CambiarEstadoCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.primaryports.dto.CambiarEstadoCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.primaryports.mapper.CambiarEstadoCoordinadorMapper;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.usecase.CambiarEstadoCoordinadorUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.common.dto.ResultadoGestionCoordinadorDTO;

import java.util.Objects;

public final class CambiarEstadoCoordinadorInteractor implements CambiarEstadoCoordinadorInputPort {

    private final CambiarEstadoCoordinadorUseCase useCase;

    public CambiarEstadoCoordinadorInteractor(final CambiarEstadoCoordinadorUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "CambiarEstadoCoordinadorUseCase es obligatorio.");
    }

    @Override
    public ResultadoGestionCoordinadorDTO execute(final CambiarEstadoCoordinadorDTO dto) {
        return CambiarEstadoCoordinadorMapper.toDTO(useCase.execute(CambiarEstadoCoordinadorMapper.toDomain(dto)));
    }
}
