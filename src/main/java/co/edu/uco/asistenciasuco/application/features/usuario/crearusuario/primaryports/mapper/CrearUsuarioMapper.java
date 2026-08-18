package co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.dto.CrearUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.dto.CrearUsuarioResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.domain.CrearUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.entity.CrearUsuarioResultadoEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Mapper para convertir entre DTOs y modelos internos de crear usuario.
 */
public final class CrearUsuarioMapper {

    private CrearUsuarioMapper() {
        throw new CrosscuttingException("No es permitido instanciar una clase utilitaria.");
    }

    public static CrearUsuarioDomain toDomain(final CrearUsuarioDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para crear usuario es obligatorio.");
        }

        return CrearUsuarioDomain.crear(
                dto.getTipoIdIdentificacion(),
                dto.getNumeroIdentificacion(),
                dto.getPrimerApellido(),
                dto.getSegundoApellido(),
                dto.getPrimerNombre(),
                dto.getSegundoNombre(),
                dto.getCorreo(),
                dto.getPassword()
        );
    }

    public static CrearUsuarioResultadoDTO toDTO(final CrearUsuarioResultadoEntity entity) {
        if (ObjectHelper.isNull(entity)) {
            throw new CrosscuttingException("El resultado de crear usuario es obligatorio.");
        }

        return new CrearUsuarioResultadoDTO(
                entity.isExitoso(),
                entity.getMensajeUsuario()
        );
    }
}
