package co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.CrearGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.dto.CrearGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.dto.CrearGrupoResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.mapper.CrearGrupoMapper;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.CrearGrupoUseCase;

import java.util.Objects;

public final class CrearGrupoInteractor implements CrearGrupoInputPort {

    private final CrearGrupoUseCase useCase;

    public CrearGrupoInteractor(final CrearGrupoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso CrearGrupoUseCase es obligatorio.");
    }

    @Override
    public CrearGrupoResultadoDTO execute(final CrearGrupoDTO dto) {
        return CrearGrupoMapper.toDTO(useCase.execute(CrearGrupoMapper.toDomain(dto)));
    }
}
