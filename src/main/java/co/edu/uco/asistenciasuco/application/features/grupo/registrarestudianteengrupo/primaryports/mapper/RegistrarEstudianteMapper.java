package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain.RegistrarEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.entity.RegistrarEstudianteResultadoEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Mapper para convertir el DTO de entrada al dominio del caso de uso.
 */
public final class RegistrarEstudianteMapper {

    private RegistrarEstudianteMapper() {
        throw new CrosscuttingException("No es permitido instanciar una clase utilitaria.");
    }

    public static RegistrarEstudianteDomain toDomain(final RegistrarEstudianteDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para registrar estudiante en grupo es obligatorio.");
        }

        return new RegistrarEstudianteDomain(
                dto.getTipoIdentificacionId(),
                dto.getNumeroIdentificacion(),
                dto.getPrimerApellido(),
                dto.getSegundoApellido(),
                dto.getPrimerNombre(),
                dto.getSegundoNombre(),
                dto.getCorreo(),
                dto.getPassword(),
                dto.getGrupoId()
        );
    }

    public static RegistrarEstudianteResultadoDTO toDTO(final RegistrarEstudianteResultadoEntity entity) {
        if (ObjectHelper.isNull(entity)) {
            throw new CrosscuttingException("El resultado de registrar estudiante en grupo es obligatorio.");
        }

        return new RegistrarEstudianteResultadoDTO(entity.isExitoso(), entity.getMensajeUsuario());
    }
}
