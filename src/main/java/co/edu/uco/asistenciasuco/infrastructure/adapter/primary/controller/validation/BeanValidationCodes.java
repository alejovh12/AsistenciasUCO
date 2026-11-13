package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

import jakarta.validation.Payload;

/**
 * ROL-06: {@code payload} de Bean Validation que fija el {@code details[].code} cuando el validator propio usa un codigo
 * distinto del que corresponde por defecto a la anotacion (por ejemplo {@code @Positive} se reporta como
 * {@code FIELD_OUT_OF_RANGE}, pero el numero de identificacion no positivo es {@code FIELD_INVALID_FORMAT}).
 *
 * <pre>{@code @Positive(message = "...", payload = BeanValidationCodes.InvalidFormat.class)}</pre>
 */
public final class BeanValidationCodes {

    /** {@code FIELD_REQUIRED}. */
    public interface Required extends Payload {
    }

    /** {@code FIELD_INVALID_FORMAT}. */
    public interface InvalidFormat extends Payload {
    }

    /** {@code FIELD_INVALID_LENGTH}. */
    public interface InvalidLength extends Payload {
    }

    /** {@code FIELD_OUT_OF_RANGE}. */
    public interface OutOfRange extends Payload {
    }

    private BeanValidationCodes() {
    }
}
