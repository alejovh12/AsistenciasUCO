package co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.usecase.domain.AsignarDocenteAGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.usecase.entity.AsignarDocenteAGrupoResultadoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.AsignarDocenteAGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteOperacionRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Mapper entre el dominio de asignar docente a grupo y el puerto secundario.
 */
public final class AsignarDocenteAGrupoRepositoryMapper {

    private AsignarDocenteAGrupoRepositoryMapper() {
    }

    public static AsignarDocenteAGrupoRepositoryDTO toRepositoryDTO(final AsignarDocenteAGrupoDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para asignar docente a grupo es obligatorio.");
        }

        return new AsignarDocenteAGrupoRepositoryDTO(
                domain.getDocente(),
                domain.getGrupo()
        );
    }

    public static AsignarDocenteAGrupoResultadoEntity toUseCaseEntity(
            final DocenteOperacionRepositoryProjection repositoryEntity,
            final AsignarDocenteAGrupoDomain domain
    ) {
        if (ObjectHelper.isNull(repositoryEntity)) {
            throw new CrosscuttingException("El resultado de asignar docente a grupo es obligatorio.");
        }
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para asignar docente a grupo es obligatorio.");
        }

        return new AsignarDocenteAGrupoResultadoEntity(true, repositoryEntity.getMensajeUsuario());
    }
}
