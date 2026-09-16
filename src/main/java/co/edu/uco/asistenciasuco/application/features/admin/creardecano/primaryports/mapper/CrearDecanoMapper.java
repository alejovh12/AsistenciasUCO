package co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.dto.CrearDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.domain.CrearDecanoDomain;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.exception.TipoIdentificacionErrorCode;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

/**
 * Mapper entre el DTO de entrada y el dominio de crear decano.
 */
public final class CrearDecanoMapper {

    private CrearDecanoMapper() {
    }

    public static CrearDecanoDomain toDomain(final CrearDecanoDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para crear decano es obligatorio.");
        }
        if (dto.tipoIdentificacionId() == null) {
            throw new ValidationException(TipoIdentificacionErrorCode.ERR_TIPO_IDENTIFICACION_REQUERIDA);
        }
        if (dto.usuarioEjecutor() == null) {
            throw new ValidationException(UsuarioErrorCode.ERR_USUARIO_REQUERIDO);
        }

        return new CrearDecanoDomain(
                dto.tipoIdentificacionId(),
                dto.numeroIdentificacion(),
                dto.primerNombre(),
                dto.segundoNombre(),
                dto.primerApellido(),
                dto.segundoApellido(),
                dto.correo(),
                dto.password(),
                dto.idFacultad(),
                dto.usuarioEjecutor()
        );
    }
}
