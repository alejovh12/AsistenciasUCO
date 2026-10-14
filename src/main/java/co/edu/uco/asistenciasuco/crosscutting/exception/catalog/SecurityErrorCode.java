package co.edu.uco.asistenciasuco.crosscutting.exception.catalog;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

public enum SecurityErrorCode implements ErrorDefinition {
    UNAUTHORIZED("UNAUTHORIZED", "Debe autenticarse para realizar esta operacion.", ErrorKind.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "No tiene permisos para realizar esta operacion.", ErrorKind.FORBIDDEN),
    // SEC-BASELINE-001 (experimental): solo se emite con app.providers.security-baseline-pep.enabled=true,
    // cuando el PEP externo no puede confirmar una decision. Requiere decision contractual antes de merge.
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
