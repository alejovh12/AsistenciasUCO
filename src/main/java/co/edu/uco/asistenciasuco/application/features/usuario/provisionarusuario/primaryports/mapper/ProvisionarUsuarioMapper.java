package co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.dto.ProvisionarUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.dto.ProvisionarUsuarioResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.domain.ProvisionarUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.entity.ProvisionarUsuarioResultadoEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

/**
 * Mapper para convertir entre DTOs y modelos internos de provisionar usuario.
 */
public final class ProvisionarUsuarioMapper {

    private ProvisionarUsuarioMapper() {
    }

    public static ProvisionarUsuarioDomain toDomain(final ProvisionarUsuarioDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para provisionar usuario es obligatorio.");
        }

        return ProvisionarUsuarioDomain.crear(
                dto.tipoIdIdentificacion(),
                dto.numeroIdentificacion(),
                dto.primerApellido(),
                dto.segundoApellido(),
                dto.primerNombre(),
                dto.segundoNombre(),
                dto.correo(),
                dto.password()
        );
    }

    public static ProvisionarUsuarioResultadoDTO toDTO(final ProvisionarUsuarioResultadoEntity entity) {
        if (ObjectHelper.isNull(entity)) {
            throw new CrosscuttingException("El resultado de provisionar usuario es obligatorio.");
        }

        return new ProvisionarUsuarioResultadoDTO(
                entity.getUsuarioId(),
                entity.isExitoso(),
                entity.getMensajeUsuario()
        );
    }
}
