package co.edu.uco.asistenciasuco.application.exception;

public class InternalApplicationException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public InternalApplicationException(final String message) {
        super(ErrorCode.INTERNAL_APPLICATION_ERROR, message);
    }

    public InternalApplicationException(final ErrorCode errorCode) {
        super(errorCode);
    }

    public InternalApplicationException(final ErrorCode errorCode, final String technicalMessage) {
        super(errorCode, technicalMessage);
    }

    public InternalApplicationException(final String message, final Throwable cause) {
        super(ErrorCode.INTERNAL_APPLICATION_ERROR, message, cause);
    }

    public InternalApplicationException(final String code, final String message) {
        super(code, message);
    }

    protected InternalApplicationException(final ErrorCode errorCode, final String message, final Throwable cause) {
        super(errorCode, message, cause);
    }

    protected InternalApplicationException(final String code, final String message, final Throwable cause) {
        super(code, message, cause);
    }
}
