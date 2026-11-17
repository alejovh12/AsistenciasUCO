package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

public enum DatabaseErrorCode implements ErrorDefinition {
    DATABASE_OPERATION_ERROR("DATABASE_OPERATION_ERROR", Mensajes.ERROR_INTERNO_SOPORTE, ErrorKind.TECHNICAL),
    FEATURE_UNAVAILABLE("FEATURE_UNAVAILABLE", "La operacion solicitada no esta soportada.", ErrorKind.BUSINESS),
    ERR_DB_CANONICAL_CONTRACT("ERR_DB_CANONICAL_CONTRACT", Mensajes.ERROR_INTERNO_SOPORTE, ErrorKind.TECHNICAL),
    ERR_DB_CATALOG_INCONSISTENT("ERR_DB_CATALOG_INCONSISTENT", Mensajes.ERROR_INTERNO_SOPORTE, ErrorKind.TECHNICAL),
    ERR_DB_UNCLASSIFIED("ERR_DB_UNCLASSIFIED", Mensajes.ERROR_INTERNO_SOPORTE, ErrorKind.TECHNICAL);

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

    private static final class Mensajes {
        static final String ERROR_INTERNO_SOPORTE = "Ocurrio un error interno. Utilice el codigo de seguimiento para soporte.";
    }
}
