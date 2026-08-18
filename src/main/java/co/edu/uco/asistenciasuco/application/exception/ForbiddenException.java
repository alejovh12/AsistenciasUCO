package co.edu.uco.asistenciasuco.application.exception;

public final class ForbiddenException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException(final String message) {
        super("FORBIDDEN", message);
    }

    public ForbiddenException(final String code, final String message) {
        super(code, message);
    }
}
