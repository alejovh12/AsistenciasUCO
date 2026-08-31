package co.edu.uco.asistenciasuco.application.features.usuario.domain.rules;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.helpers.validation.ValidationHelper;

public final class NombrePersonaRule {

    public static final int MAX_LENGTH = 50;

    private NombrePersonaRule() {
    }

    public static String validarObligatorio(final String value, final UsuarioErrorCode requiredCode) {
        if (!ValidationHelper.hasText(value)) {
            throw new ValidationException(requiredCode);
        }
        return validarFormatoYLongitud(value);
    }

    public static String validarOpcional(final String value) {
        if (!ValidationHelper.hasText(value)) {
            return "";
        }
        return validarFormatoYLongitud(value);
    }

    private static String validarFormatoYLongitud(final String value) {
        if (!ValidationHelper.isLengthAtMost(value, MAX_LENGTH)) {
            throw new ValidationException(UsuarioErrorCode.ERR_NOMBRE_PERSONA_LONGITUD_INVALIDA);
        }
        if (!ValidationHelper.isValidPersonName(value)) {
            throw new ValidationException(UsuarioErrorCode.ERR_NOMBRE_PERSONA_INVALIDO);
        }
        return value;
    }
}
