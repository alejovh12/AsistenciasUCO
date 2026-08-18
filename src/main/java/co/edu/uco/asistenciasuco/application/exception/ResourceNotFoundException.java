package co.edu.uco.asistenciasuco.application.exception;

public final class ResourceNotFoundException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(final String message) {
        super("RESOURCE_NOT_FOUND", message);
    }

    public ResourceNotFoundException(final String code, final String message) {
        super(code, message);
    }
}
