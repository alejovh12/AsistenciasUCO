package co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.ConsultarGruposUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.entity.GrupoEntity;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.mapper.ConsultarGruposRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.List;

/**
 * Implementacion del caso de uso consultar grupos.
 */
public final class ConsultarGruposUseCaseImpl implements ConsultarGruposUseCase {

    private final GrupoRepositoryPort grupoRepositoryPort;

    public ConsultarGruposUseCaseImpl(final GrupoRepositoryPort grupoRepositoryPort) {
        if (ObjectHelper.isNull(grupoRepositoryPort)) {
            throw new CrosscuttingException("El puerto de salida GrupoRepositoryPort es obligatorio.");
        }
        this.grupoRepositoryPort = grupoRepositoryPort;
    }

    @Override
    public List<GrupoEntity> execute() {
        return ConsultarGruposRepositoryMapper.toUseCaseEntities(grupoRepositoryPort.consultarGrupos());
    }
}
