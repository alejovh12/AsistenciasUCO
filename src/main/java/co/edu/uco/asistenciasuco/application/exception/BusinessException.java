package co.edu.uco.asistenciasuco.application.exception;

public class BusinessException extends ApplicationException {

    private static final long serialVersionUID = 1L;
    private static final String CODE = "BUSINESS_ERROR";

    public BusinessException(final String message) {
        super(CODE, message);
    }

    protected BusinessException(final String code, final String message) {
        super(code, message);
    }
}
