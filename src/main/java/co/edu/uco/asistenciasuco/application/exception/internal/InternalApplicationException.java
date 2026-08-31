package co.edu.uco.asistenciasuco.application.exception.internal;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;

public class InternalApplicationException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public InternalApplicationException(final String message) {
        super(CommonErrorCode.INTERNAL_APPLICATION_ERROR, message);
    }

    public InternalApplicationException(final ErrorDefinition errorDefinition) {
        super(errorDefinition);
    }

    public InternalApplicationException(final ErrorDefinition errorDefinition, final String technicalMessage) {
        super(errorDefinition, technicalMessage);
    }

    public InternalApplicationException(final String message, final Throwable cause) {
        super(CommonErrorCode.INTERNAL_APPLICATION_ERROR, message, cause);
    }

    public InternalApplicationException(final String code, final String message) {
        super(code, message);
    }

    protected InternalApplicationException(final ErrorDefinition errorDefinition, final String message, final Throwable cause) {
        super(errorDefinition, message, cause);
    }

    protected InternalApplicationException(final String code, final String message, final Throwable cause) {
        super(code, message, cause);
    }
}
