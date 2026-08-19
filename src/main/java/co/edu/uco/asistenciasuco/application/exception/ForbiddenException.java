package co.edu.uco.asistenciasuco.application.exception;

public final class ForbiddenException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException(final String message) {
        super(ErrorCode.FORBIDDEN, message);
    }

    public ForbiddenException(final ErrorCode errorCode) {
        super(errorCode);
    }

    public ForbiddenException(final ErrorCode errorCode, final String technicalMessage) {
        super(errorCode, technicalMessage);
    }

    public ForbiddenException(final String code, final String message) {
        super(code, message);
    }
}
