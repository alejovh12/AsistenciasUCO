package co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.ConsultarDocentesUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.entity.DocenteIdentidadEntity;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.mapper.ConsultarDocentesRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.DocenteRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.List;

/**
 * Implementacion del caso de uso consultar docentes.
 */
public final class ConsultarDocentesUseCaseImpl implements ConsultarDocentesUseCase {

    private final DocenteRepositoryPort docenteRepositoryPort;

    public ConsultarDocentesUseCaseImpl(final DocenteRepositoryPort docenteRepositoryPort) {
        if (ObjectHelper.isNull(docenteRepositoryPort)) {
            throw new CrosscuttingException("El puerto de salida DocenteRepositoryPort es obligatorio.");
        }
        this.docenteRepositoryPort = docenteRepositoryPort;
    }

    @Override
    public List<DocenteIdentidadEntity> execute() {
        return ConsultarDocentesRepositoryMapper.toUseCaseEntities(docenteRepositoryPort.consultarDocentes());
    }
}
