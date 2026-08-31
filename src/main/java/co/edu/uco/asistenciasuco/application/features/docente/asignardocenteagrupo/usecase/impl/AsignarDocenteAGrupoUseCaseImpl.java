package co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.usecase.AsignarDocenteAGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.usecase.domain.AsignarDocenteAGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.usecase.entity.AsignarDocenteAGrupoResultadoEntity;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.usecase.mapper.AsignarDocenteAGrupoRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DocenteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteOperacionRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import java.util.Objects;

/**
 * Implementacion del caso de uso asignar docente a grupo.
 */
public final class AsignarDocenteAGrupoUseCaseImpl implements AsignarDocenteAGrupoUseCase {

    private final DocenteRepositoryPort docenteRepositoryPort;

    public AsignarDocenteAGrupoUseCaseImpl(final DocenteRepositoryPort docenteRepositoryPort) {
        this.docenteRepositoryPort = Objects.requireNonNull(docenteRepositoryPort, "El puerto de salida DocenteRepositoryPort es obligatorio.");
    }

    @Override
    public AsignarDocenteAGrupoResultadoEntity execute(final AsignarDocenteAGrupoDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para asignar docente a grupo es obligatorio.");
        }

        final DocenteOperacionRepositoryProjection resultado = docenteRepositoryPort.asignarDocenteAGrupo(
                AsignarDocenteAGrupoRepositoryMapper.toRepositoryDTO(domain)
        );

        if (ObjectHelper.isNull(resultado)) {
            throw new CrosscuttingException("El resultado de asignar docente a grupo es obligatorio.");
        }

        return AsignarDocenteAGrupoRepositoryMapper.toUseCaseEntity(resultado, domain);
    }
}