package co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.ActualizarGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.dto.ActualizarGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.dto.ActualizarGrupoResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.mapper.ActualizarGrupoMapper;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.ActualizarGrupoUseCase;

import java.util.Objects;

public final class ActualizarGrupoInteractor implements ActualizarGrupoInputPort {

    private final ActualizarGrupoUseCase useCase;

    public ActualizarGrupoInteractor(final ActualizarGrupoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso ActualizarGrupoUseCase es obligatorio.");
    }

    @Override
    public ActualizarGrupoResultadoDTO execute(final ActualizarGrupoDTO dto) {
        return ActualizarGrupoMapper.toDTO(useCase.execute(ActualizarGrupoMapper.toDomain(dto)));
    }
}
