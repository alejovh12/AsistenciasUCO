package co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.ConsultarGruposUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.entity.GrupoEntity;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.mapper.ConsultarGruposRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;

import java.util.List;
import java.util.Objects;

/**
 * Implementacion del caso de uso consultar grupos.
 */
public final class ConsultarGruposUseCaseImpl implements ConsultarGruposUseCase {

    private final GrupoRepositoryPort grupoRepositoryPort;

    public ConsultarGruposUseCaseImpl(final GrupoRepositoryPort grupoRepositoryPort) {
        this.grupoRepositoryPort = Objects.requireNonNull(grupoRepositoryPort, "El puerto de salida GrupoRepositoryPort es obligatorio.");
    }

    @Override
    public List<GrupoEntity> execute() {
        return ConsultarGruposRepositoryMapper.toUseCaseEntities(grupoRepositoryPort.consultarGrupos());
    }
}