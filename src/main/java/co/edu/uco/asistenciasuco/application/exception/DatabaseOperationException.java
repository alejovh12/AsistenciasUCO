package co.edu.uco.asistenciasuco.application.exception;

public final class DatabaseOperationException extends InternalApplicationException {

    private static final long serialVersionUID = 1L;

    public DatabaseOperationException(final String message, final Throwable cause) {
        super(ErrorCode.DATABASE_OPERATION_ERROR, message, cause);
    }
}
