package co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.entity.DocenteIdentidadEntity;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.mapper.ConsultarDocentesRepositoryMapper;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase.ConsultarDocentePorIdUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase.domain.ConsultarDocentePorIdDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase.mapper.ConsultarDocentePorIdRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.DocenteRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Implementacion del caso de uso consultar docente por ID.
 */
public final class ConsultarDocentePorIdUseCaseImpl implements ConsultarDocentePorIdUseCase {

    private final DocenteRepositoryPort docenteRepositoryPort;

    public ConsultarDocentePorIdUseCaseImpl(final DocenteRepositoryPort docenteRepositoryPort) {
        if (ObjectHelper.isNull(docenteRepositoryPort)) {
            throw new CrosscuttingException("El puerto de salida DocenteRepositoryPort es obligatorio.");
        }
        this.docenteRepositoryPort = docenteRepositoryPort;
    }

    @Override
    public DocenteIdentidadEntity execute(final ConsultarDocentePorIdDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para consultar docente por ID es obligatorio.");
        }

        return docenteRepositoryPort.consultarDocentePorId(
                        ConsultarDocentePorIdRepositoryMapper.toRepositoryDTO(domain)
                )
                .map(ConsultarDocentesRepositoryMapper::toUseCaseEntity)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ERR_DOCENTE_NO_EXISTE",
                        "El docente consultado no existe."
                ));
    }
}
