package co.edu.uco.asistenciasuco.application.exception;

public final class ConflictException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ConflictException(final String message) {
        super("CONFLICT", message);
    }

    public ConflictException(final String code, final String message) {
        super(code, message);
    }
}
