package co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.domain.RegistrarDocenteDesdeUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.entity.RegistrarDocenteDesdeUsuarioResultadoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarDocenteDesdeUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteOperacionRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Mapper entre el dominio de registrar docente desde usuario y el puerto secundario.
 */
public final class RegistrarDocenteDesdeUsuarioRepositoryMapper {

    private RegistrarDocenteDesdeUsuarioRepositoryMapper() {
    }

    public static RegistrarDocenteDesdeUsuarioRepositoryDTO toRepositoryDTO(
            final RegistrarDocenteDesdeUsuarioDomain domain
    ) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para registrar docente desde usuario es obligatorio.");
        }

        return new RegistrarDocenteDesdeUsuarioRepositoryDTO(domain.getUsuario());
    }

    public static RegistrarDocenteDesdeUsuarioResultadoEntity toUseCaseEntity(
            final DocenteOperacionRepositoryProjection repositoryEntity,
            final RegistrarDocenteDesdeUsuarioDomain domain
    ) {
        if (ObjectHelper.isNull(repositoryEntity)) {
            throw new CrosscuttingException("El resultado de registrar docente desde usuario es obligatorio.");
        }
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para registrar docente desde usuario es obligatorio.");
        }

        return new RegistrarDocenteDesdeUsuarioResultadoEntity(
                repositoryEntity.getDocenteId(),
                true,
                repositoryEntity.getMensajeUsuario()
        );
    }
}
