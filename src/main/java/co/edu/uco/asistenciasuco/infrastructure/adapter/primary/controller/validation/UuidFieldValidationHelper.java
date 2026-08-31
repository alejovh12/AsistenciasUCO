package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

import co.edu.uco.asistenciasuco.crosscutting.helpers.validation.ValidationHelper;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;

import java.util.UUID;

public final class UuidFieldValidationHelper {

    private UuidFieldValidationHelper() {
    }

    public static void validateRequiredUuid(
            final ValidationResultBuilder builder,
            final String field,
            final UUID value,
            final String requiredMessage,
            final String invalidMessage
    ) {
        if (value == null) {
            builder.add(field, ValidationErrorType.REQUIRED, requiredMessage);
        } else if (!ValidationHelper.isNonEmptyUuid(value)) {
            builder.add(field, ValidationErrorType.INVALID_UUID, invalidMessage);
        }
    }

    public static void validateOptionalUuid(
            final ValidationResultBuilder builder,
            final String field,
            final UUID value,
            final String invalidMessage
    ) {
        if (value != null && !ValidationHelper.isNonEmptyUuid(value)) {
            builder.add(field, ValidationErrorType.INVALID_UUID, invalidMessage);
        }
    }
}
