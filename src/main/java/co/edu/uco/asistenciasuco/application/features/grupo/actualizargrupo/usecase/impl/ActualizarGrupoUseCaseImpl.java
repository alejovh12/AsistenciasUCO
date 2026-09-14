package co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.ActualizarGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.domain.ActualizarGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.entity.ActualizarGrupoResultadoEntity;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.mapper.ActualizarGrupoRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;

import java.util.Objects;

public final class ActualizarGrupoUseCaseImpl implements ActualizarGrupoUseCase {

    private final GrupoRepositoryPort grupoRepositoryPort;

    public ActualizarGrupoUseCaseImpl(final GrupoRepositoryPort grupoRepositoryPort) {
        this.grupoRepositoryPort = Objects.requireNonNull(grupoRepositoryPort, "El puerto GrupoRepositoryPort es obligatorio.");
    }

    @Override
    public ActualizarGrupoResultadoEntity execute(final ActualizarGrupoDomain domain) {
        return ActualizarGrupoRepositoryMapper.toEntity(
                grupoRepositoryPort.actualizarGrupo(ActualizarGrupoRepositoryMapper.toRepositoryDTO(domain))
        );
    }
}
