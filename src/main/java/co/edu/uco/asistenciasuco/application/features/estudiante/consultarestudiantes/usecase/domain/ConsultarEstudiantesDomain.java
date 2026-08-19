package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.ErrorCode;
import co.edu.uco.asistenciasuco.application.exception.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;

import java.util.UUID;

public record ConsultarEstudiantesDomain(
        UUID tipoIdentificacionId,
        Integer numeroIdentificacion,
        String nombre,
        String correo,
        UUID institucionId,
        UUID facultadId,
        UUID programaId,
        UUID grupoId,
        Boolean activo,
        int page,
        int size
) {

    private static final UUID EMPTY_UUID = new UUID(0L, 0L);
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 100;

    public static ConsultarEstudiantesDomain crear(
            final UUID tipoIdentificacionId,
            final Integer numeroIdentificacion,
            final String nombre,
            final String correo,
            final UUID institucionId,
            final UUID facultadId,
            final UUID programaId,
            final UUID grupoId,
            final Boolean activo,
            final Integer page,
            final Integer size
    ) {
        final int pageValue = page == null ? DEFAULT_PAGE : page;
        final int sizeValue = size == null ? DEFAULT_SIZE : size;
        if (pageValue < 0) {
            throw new ValidationException(ErrorCode.ERR_PAGE_INVALIDA);
        }
        if (sizeValue < MIN_SIZE || sizeValue > MAX_SIZE) {
            throw new ValidationException(ErrorCode.ERR_SIZE_INVALIDO);
        }

        return new ConsultarEstudiantesDomain(
                validarUuidOpcional(tipoIdentificacionId, ErrorCode.ERR_TIPO_IDENTIFICACION_INVALIDA),
                numeroIdentificacion,
                TextHelper.trim(nombre),
                TextHelper.normalizeTrimLower(correo),
                validarUuidOpcional(institucionId, ErrorCode.ERR_INSTITUCION_INVALIDA),
                validarUuidOpcional(facultadId, ErrorCode.ERR_FACULTAD_INVALIDA),
                validarUuidOpcional(programaId, ErrorCode.ERR_PROGRAMA_INVALIDO),
                validarUuidOpcional(grupoId, ErrorCode.ERR_GRUPO_INVALIDO),
                activo,
                pageValue,
                sizeValue
        );
    }

    private static UUID validarUuidOpcional(final UUID value, final ErrorCode code) {
        if (EMPTY_UUID.equals(value)) {
            throw new ValidationException(code);
        }
        return value;
    }
}
