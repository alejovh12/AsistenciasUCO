package co.edu.uco.asistenciasuco.application.features.sesioncalendario.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

/**
 * ATA D7: codigos del calendario de sesiones (horario semanal con fechas). Se exponen tal cual en {@code ApiErrorResponse.code}; el
 * texto que ve el usuario sale del catalogo de mensajes ({@code CAL_*}) y este texto es el respaldo ante desconexion.
 */
public enum CalendarioErrorCode implements ErrorDefinition {
    CAL_001("CAL_001",
            "El rango de fechas no es válido: indique inicio y fin, con el fin igual o posterior y máximo 31 días.",
            ErrorKind.VALIDATION);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    CalendarioErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
