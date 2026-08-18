package co.edu.uco.asistenciasuco.application.exception;

/**
 * Excepcion base para errores controlados de aplicacion.
 */
public abstract class ApplicationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    protected ApplicationException(final String code, final String message) {
        super(message);
        this.code = code;
    }

    protected ApplicationException(final String code, final String message, final Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
