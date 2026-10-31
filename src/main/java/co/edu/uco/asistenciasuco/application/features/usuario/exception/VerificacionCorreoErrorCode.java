package co.edu.uco.asistenciasuco.application.features.usuario.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

/**
 * IDENT-001: codigos de error del correo institucional y la verificacion del correo. Los mensajes por defecto son el
 * respaldo de los textos del catalogo ({@code VCE_001}, {@code VCE_002}, {@code VCE_005}, {@code VCE_007}); VCE_002 es el
 * mismo para cualquier token invalido, de modo que no revela si existio, vencio o ya se uso.
 */
public enum VerificacionCorreoErrorCode implements ErrorDefinition {
    VCE_001("VCE_001", "El correo del estudiante debe ser institucional.", ErrorKind.VALIDATION),
    VCE_002("VCE_002", "El enlace de verificación no es válido o ya venció.", ErrorKind.VALIDATION),
    VCE_005("VCE_005", "El correo de este estudiante ya fue verificado.", ErrorKind.CONFLICT),
    VCE_007("VCE_007", "El usuario no existe o no está activo.", ErrorKind.NOT_FOUND);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    VerificacionCorreoErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.kind = kind;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }

    @Override
    public ErrorKind kind() {
        return kind;
    }
}
