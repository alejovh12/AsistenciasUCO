package co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.CrearGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.domain.CrearGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.entity.CrearGrupoResultadoEntity;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.mapper.CrearGrupoRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;

import java.util.Objects;
import java.util.UUID;

public final class CrearGrupoUseCaseImpl implements CrearGrupoUseCase {

    private final GrupoRepositoryPort grupoRepositoryPort;

    public CrearGrupoUseCaseImpl(final GrupoRepositoryPort grupoRepositoryPort) {
        this.grupoRepositoryPort = Objects.requireNonNull(grupoRepositoryPort, "El puerto GrupoRepositoryPort es obligatorio.");
    }

    @Override
    public CrearGrupoResultadoEntity execute(final CrearGrupoDomain domain) {
        if (domain.generarSesionesAutomaticas()) {
            throw new FeatureUnavailableException(
                    "La generacion automatica de sesiones requiere que la DB publique primero la gestion de horario del grupo."
            );
        }
        final UUID idGrupo = UUID.randomUUID();
        final var projection = grupoRepositoryPort.crearGrupo(CrearGrupoRepositoryMapper.toRepositoryDTO(domain, idGrupo));
        return CrearGrupoRepositoryMapper.toEntity(projection);
    }
}
