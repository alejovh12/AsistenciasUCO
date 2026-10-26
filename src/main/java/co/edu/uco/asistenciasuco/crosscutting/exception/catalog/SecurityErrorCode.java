package co.edu.uco.asistenciasuco.crosscutting.exception.catalog;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

public enum SecurityErrorCode implements ErrorDefinition {
    UNAUTHORIZED("UNAUTHORIZED", "Debe autenticarse para realizar esta operacion.", ErrorKind.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "No tiene permisos para realizar esta operacion.", ErrorKind.FORBIDDEN),
    // SEC-BASELINE-001: solo se emite cuando el PEP baseline no puede confirmar una decisión;
    // nunca se emite en el provider legacy.
    ACCESS_DECISION_UNAVAILABLE("ACCESS_DECISION_UNAVAILABLE",
            "No fue posible verificar la autorizacion en este momento. Intente nuevamente.", ErrorKind.TECHNICAL);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    SecurityErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
