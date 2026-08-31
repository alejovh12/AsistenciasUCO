package co.edu.uco.asistenciasuco.application.exception.business;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;

public final class ConflictException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ConflictException(final String message) {
        super(CommonErrorCode.CONFLICT, message);
    }

    public ConflictException(final ErrorDefinition errorDefinition) {
        super(errorDefinition);
    }

    public ConflictException(final ErrorDefinition errorDefinition, final String technicalMessage) {
        super(errorDefinition, technicalMessage);
    }

    public ConflictException(final String code, final String message) {
        super(code, message);
    }
}
