package co.edu.uco.asistenciasuco.application.exception;

public final class ValidationException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ValidationException(final String message) {
        super("VALIDATION_ERROR", message);
    }

    public ValidationException(final String code, final String message) {
        super(code, message);
    }
}
