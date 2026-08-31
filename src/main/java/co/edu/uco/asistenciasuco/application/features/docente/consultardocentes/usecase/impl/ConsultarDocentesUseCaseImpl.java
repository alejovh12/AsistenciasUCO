package co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.ConsultarDocentesUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.entity.DocenteIdentidadEntity;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.mapper.ConsultarDocentesRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DocenteRepositoryPort;

import java.util.List;
import java.util.Objects;

/**
 * Implementacion del caso de uso consultar docentes.
 */
public final class ConsultarDocentesUseCaseImpl implements ConsultarDocentesUseCase {

    private final DocenteRepositoryPort docenteRepositoryPort;

    public ConsultarDocentesUseCaseImpl(final DocenteRepositoryPort docenteRepositoryPort) {
        this.docenteRepositoryPort = Objects.requireNonNull(docenteRepositoryPort, "El puerto de salida DocenteRepositoryPort es obligatorio.");
    }

    @Override
    public List<DocenteIdentidadEntity> execute() {
        return ConsultarDocentesRepositoryMapper.toUseCaseEntities(docenteRepositoryPort.consultarDocentes());
    }
}