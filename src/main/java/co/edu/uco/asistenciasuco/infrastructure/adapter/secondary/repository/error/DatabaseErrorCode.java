package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

public enum DatabaseErrorCode implements ErrorDefinition {
    DATABASE_OPERATION_ERROR("DATABASE_OPERATION_ERROR", "Ocurrio un error interno. Utilice el codigo de seguimiento para soporte.", ErrorKind.TECHNICAL),
    ERR_DB_UNCLASSIFIED("ERR_DB_UNCLASSIFIED", "Ocurrio un error interno. Utilice el codigo de seguimiento para soporte.", ErrorKind.TECHNICAL);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    DatabaseErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
