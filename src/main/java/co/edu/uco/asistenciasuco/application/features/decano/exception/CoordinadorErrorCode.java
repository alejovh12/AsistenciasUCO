package co.edu.uco.asistenciasuco.application.features.decano.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

/**
 * Errores de negocio de la gestion del coordinador por el decano. El codigo coincide con el del catalogo de
 * mensajes (DB / App Configuration), de modo que el usuario recibe el texto institucional y no uno generico.
 */
public enum CoordinadorErrorCode implements ErrorDefinition {
    COO_001("COO_001",
            "No se puede desactivar al coordinador mientras tenga un programa activo a cargo. Reasigne o desactive el programa primero.",
            ErrorKind.CONFLICT);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    CoordinadorErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
