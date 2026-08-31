package co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.domain.CrearUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.entity.CrearUsuarioResultadoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CrearUsuarioRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Mapper entre el dominio de crear usuario y el puerto secundario.
 */
public final class CrearUsuarioRepositoryMapper {

    private CrearUsuarioRepositoryMapper() {
    }

    public static CrearUsuarioRepositoryDTO toRepositoryDTO(final CrearUsuarioDomain domain) {
        return toRepositoryDTO(domain, domain.getPassword());
    }

    public static CrearUsuarioRepositoryDTO toRepositoryDTO(final CrearUsuarioDomain domain, final String password) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para crear usuario es obligatorio.");
        }

        return new CrearUsuarioRepositoryDTO(
                domain.getTipoIdentificacionId(),
                domain.getNumeroIdentificacion(),
                domain.getPrimerApellido(),
                domain.getSegundoApellido(),
                domain.getPrimerNombre(),
                domain.getSegundoNombre(),
                domain.getCorreo(),
                password
        );
    }

    public static CrearUsuarioResultadoEntity toUseCaseEntity(
            final CrearUsuarioRepositoryProjection repositoryEntity,
            final CrearUsuarioDomain domain
    ) {
        if (ObjectHelper.isNull(repositoryEntity)) {
            throw new CrosscuttingException("El resultado de crear usuario es obligatorio.");
        }
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para crear usuario es obligatorio.");
        }

        return new CrearUsuarioResultadoEntity(
                repositoryEntity.getUsuarioId(),
                true,
                repositoryEntity.getMensajeUsuario()
        );
    }
}
