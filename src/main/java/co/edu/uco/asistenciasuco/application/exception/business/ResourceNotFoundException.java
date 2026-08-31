package co.edu.uco.asistenciasuco.application.exception.business;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;

public final class ResourceNotFoundException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(final String message) {
        super(CommonErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public ResourceNotFoundException(final ErrorDefinition errorDefinition) {
        super(errorDefinition);
    }

    public ResourceNotFoundException(final ErrorDefinition errorDefinition, final String technicalMessage) {
        super(errorDefinition, technicalMessage);
    }

    public ResourceNotFoundException(final String code, final String message) {
        super(code, message);
    }
}
