package co.edu.uco.asistenciasuco.crosscutting.exception;

import java.util.Optional;

public class TechnicalException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorDefinition errorDefinition;

    public TechnicalException(final ErrorDefinition errorDefinition) {
        super(errorDefinition.defaultMessage());
        this.errorDefinition = errorDefinition;
    }

    public TechnicalException(final ErrorDefinition errorDefinition, final String message) {
        super(message);
        this.errorDefinition = errorDefinition;
    }

    public TechnicalException(final ErrorDefinition errorDefinition, final String message, final Throwable cause) {
        super(message, cause);
        this.errorDefinition = errorDefinition;
    }

    public TechnicalException(final String message) {
        super(message);
        this.errorDefinition = null;
    }

    public TechnicalException(final String message, final Throwable cause) {
        super(message, cause);
        this.errorDefinition = null;
    }

    public Optional<ErrorDefinition> getErrorDefinition() {
        return Optional.ofNullable(errorDefinition);
    }

    public String getCode() {
        return errorDefinition == null ? null : errorDefinition.code();
    }
}
