package co.edu.uco.asistenciasuco.application.exception;

public class BusinessException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public BusinessException(final String message) {
        super(ErrorCode.BUSINESS_ERROR, message);
    }

    protected BusinessException(final ErrorCode errorCode) {
        super(errorCode);
    }

    protected BusinessException(final String code, final String message) {
        super(code, message);
    }

    protected BusinessException(final ErrorCode errorCode, final String message) {
        super(errorCode, message);
    }
}
