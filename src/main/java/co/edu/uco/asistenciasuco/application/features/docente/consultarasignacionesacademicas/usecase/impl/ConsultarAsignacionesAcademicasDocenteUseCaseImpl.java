package co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.ConsultarAsignacionesAcademicasDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.domain.ConsultarAsignacionesAcademicasDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.entity.DocenteAsignacionAcademicaEntity;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.mapper.ConsultarAsignacionesAcademicasDocenteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DocenteRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.List;
import java.util.Objects;

/**
 * Implementacion del caso de uso consultar asignaciones academicas de un docente.
 */
public final class ConsultarAsignacionesAcademicasDocenteUseCaseImpl
        implements ConsultarAsignacionesAcademicasDocenteUseCase {

    private final DocenteRepositoryPort docenteRepositoryPort;

    public ConsultarAsignacionesAcademicasDocenteUseCaseImpl(final DocenteRepositoryPort docenteRepositoryPort) {
        this.docenteRepositoryPort = Objects.requireNonNull(docenteRepositoryPort, "El puerto de salida DocenteRepositoryPort es obligatorio.");
    }

    @Override
    public List<DocenteAsignacionAcademicaEntity> execute(
            final ConsultarAsignacionesAcademicasDocenteDomain domain
    ) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException(
                    "El dominio para consultar asignaciones academicas del docente es obligatorio."
            );
        }

        return ConsultarAsignacionesAcademicasDocenteRepositoryMapper.toUseCaseEntities(
                docenteRepositoryPort.consultarAsignacionesAcademicas(
                        ConsultarAsignacionesAcademicasDocenteRepositoryMapper.toRepositoryDTO(domain)
                )
        );
    }
}