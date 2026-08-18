package co.edu.uco.asistenciasuco.application.exception;

public class InternalApplicationException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public InternalApplicationException(final String message) {
        super("INTERNAL_APPLICATION_ERROR", message);
    }

    public InternalApplicationException(final String message, final Throwable cause) {
        super("INTERNAL_APPLICATION_ERROR", message, cause);
    }

    public InternalApplicationException(final String code, final String message) {
        super(code, message);
    }

    protected InternalApplicationException(final String code, final String message, final Throwable cause) {
        super(code, message, cause);
    }
}
