package co.edu.uco.asistenciasuco.application.exception;

/**
 * Excepcion base para errores controlados de aplicacion.
 */
public abstract class ApplicationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;
    private final String code;

    protected ApplicationException(final ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
        this.code = errorCode.code();
    }

    protected ApplicationException(final ErrorCode errorCode, final String message) {
        super(message);
        this.errorCode = errorCode;
        this.code = errorCode.code();
    }

    protected ApplicationException(final ErrorCode errorCode, final String message, final Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.code = errorCode.code();
    }

    protected ApplicationException(final String code, final String message) {
        super(message);
        this.errorCode = null;
        this.code = code;
    }

    protected ApplicationException(final String code, final String message, final Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public java.util.Optional<ErrorCode> getErrorCode() {
        if (errorCode != null) {
            return java.util.Optional.of(errorCode);
        }
        return ErrorCode.fromCode(code);
    }
}
