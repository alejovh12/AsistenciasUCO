package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain.RegistrarEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.entity.RegistrarEstudianteResultadoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.RegistrarEstudianteRepositoryEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Mapper entre el dominio del caso de uso y el contrato del puerto secundario.
 */
public final class RegistrarEstudianteRepositoryMapper {

    private RegistrarEstudianteRepositoryMapper() {
        throw new CrosscuttingException("No es permitido instanciar una clase utilitaria.");
    }

    public static RegistrarEstudianteRepositoryDTO toRepositoryDTO(final RegistrarEstudianteDomain domain) {
        return toRepositoryDTO(domain, domain.getPassword());
    }

    public static RegistrarEstudianteRepositoryDTO toRepositoryDTO(
            final RegistrarEstudianteDomain domain,
            final String password
    ) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para registrar estudiante en grupo es obligatorio.");
        }

        return new RegistrarEstudianteRepositoryDTO(
                domain.getTipoIdentificacionId(),
                domain.getNumeroIdentificacion(),
                domain.getPrimerApellido(),
                domain.getSegundoApellido(),
                domain.getPrimerNombre(),
                domain.getSegundoNombre(),
                domain.getCorreo(),
                password,
                domain.getGrupoId()
        );
    }

    public static RegistrarEstudianteResultadoEntity toUseCaseEntity(
            final RegistrarEstudianteRepositoryEntity repositoryEntity
    ) {
        if (ObjectHelper.isNull(repositoryEntity)) {
            throw new CrosscuttingException("El resultado de registrar estudiante en grupo es obligatorio.");
        }

        return new RegistrarEstudianteResultadoEntity(true, repositoryEntity.getMensajeUsuario());
    }
}
