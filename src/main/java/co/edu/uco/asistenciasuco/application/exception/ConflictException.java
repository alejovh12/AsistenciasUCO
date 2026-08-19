package co.edu.uco.asistenciasuco.application.exception;

public final class ConflictException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ConflictException(final String message) {
        super(ErrorCode.CONFLICT, message);
    }

    public ConflictException(final ErrorCode errorCode) {
        super(errorCode);
    }

    public ConflictException(final ErrorCode errorCode, final String technicalMessage) {
        super(errorCode, technicalMessage);
    }

    public ConflictException(final String code, final String message) {
        super(code, message);
    }
}
