package co.edu.uco.asistenciasuco.application.exception;

public final class ResourceNotFoundException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(final String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public ResourceNotFoundException(final ErrorCode errorCode) {
        super(errorCode);
    }

    public ResourceNotFoundException(final ErrorCode errorCode, final String technicalMessage) {
        super(errorCode, technicalMessage);
    }

    public ResourceNotFoundException(final String code, final String message) {
        super(code, message);
    }
}
