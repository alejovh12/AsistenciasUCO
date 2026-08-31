package co.edu.uco.asistenciasuco.application.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;

import java.util.Optional;

/**
 * Excepcion base para errores controlados de aplicacion.
 */
public abstract class ApplicationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorDefinition errorDefinition;
    private final String code;

    protected ApplicationException(final ErrorDefinition errorDefinition) {
        super(errorDefinition.defaultMessage());
        this.errorDefinition = errorDefinition;
        this.code = errorDefinition.code();
    }

    protected ApplicationException(final ErrorDefinition errorDefinition, final String message) {
        super(message);
        this.errorDefinition = errorDefinition;
        this.code = errorDefinition.code();
    }

    protected ApplicationException(final ErrorDefinition errorDefinition, final String message, final Throwable cause) {
        super(message, cause);
        this.errorDefinition = errorDefinition;
        this.code = errorDefinition.code();
    }

    protected ApplicationException(final String code, final String message) {
        super(message);
        this.errorDefinition = null;
        this.code = code;
    }

    protected ApplicationException(final String code, final String message, final Throwable cause) {
        super(message, cause);
        this.errorDefinition = null;
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public Optional<ErrorDefinition> getErrorDefinition() {
        return Optional.ofNullable(errorDefinition);
    }
}
