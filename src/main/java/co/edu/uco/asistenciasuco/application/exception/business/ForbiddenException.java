package co.edu.uco.asistenciasuco.application.exception.business;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;

public final class ForbiddenException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException(final String message) {
        super(SecurityErrorCode.FORBIDDEN, message);
    }

    public ForbiddenException(final ErrorDefinition errorDefinition) {
        super(errorDefinition);
    }

    public ForbiddenException(final ErrorDefinition errorDefinition, final String technicalMessage) {
        super(errorDefinition, technicalMessage);
    }

    public ForbiddenException(final String code, final String message) {
        super(code, message);
    }
}
