package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error;


import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.TechnicalException;

public final class DatabaseOperationException extends TechnicalException {

    private static final long serialVersionUID = 1L;

    public DatabaseOperationException(final String message, final Throwable cause) {
        super(DatabaseErrorCode.DATABASE_OPERATION_ERROR, message, cause);
    }

    public DatabaseOperationException(final ErrorDefinition errorDefinition) {
        super(errorDefinition);
    }

    public DatabaseOperationException(final ErrorDefinition errorDefinition, final String message) {
        super(errorDefinition, message);
    }

    public DatabaseOperationException(final ErrorDefinition errorDefinition, final String message, final Throwable cause) {
        super(errorDefinition, message, cause);
    }
}
