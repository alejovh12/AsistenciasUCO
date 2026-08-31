package co.edu.uco.asistenciasuco.application.exception.business;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;

public class BusinessException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public BusinessException(final String message) {
        super(CommonErrorCode.BUSINESS_ERROR, message);
    }

    protected BusinessException(final ErrorDefinition errorDefinition) {
        super(errorDefinition);
    }

    protected BusinessException(final String code, final String message) {
        super(code, message);
    }

    protected BusinessException(final ErrorDefinition errorDefinition, final String message) {
        super(errorDefinition, message);
    }
}
