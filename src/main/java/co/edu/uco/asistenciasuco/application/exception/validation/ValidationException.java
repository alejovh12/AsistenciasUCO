package co.edu.uco.asistenciasuco.application.exception.validation;

import co.edu.uco.asistenciasuco.application.exception.business.BusinessException;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;

public final class ValidationException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ValidationException(final String message) {
        super(CommonErrorCode.VALIDATION_ERROR, message);
    }

    public ValidationException(final ErrorDefinition errorDefinition) {
        super(errorDefinition);
    }

    public ValidationException(final ErrorDefinition errorDefinition, final String technicalMessage) {
        super(errorDefinition, technicalMessage);
    }

    public ValidationException(final String code, final String message) {
        super(code, message);
    }

    private ValidationException(final ErrorDefinition errorDefinition, final Object[] argumentos) {
        super(errorDefinition, argumentos);
    }

    /** Error de catalogo con marcadores {@code {}} que se llenan con {@code argumentos} (VAL-oleada3 MSG-05). */
    public static ValidationException conParametros(final ErrorDefinition errorDefinition, final Object... argumentos) {
        return new ValidationException(errorDefinition, argumentos);
    }
}
