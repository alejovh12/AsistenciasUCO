package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.validation;

import co.edu.uco.asistenciasuco.crosscutting.helpers.validation.ValidationHelper;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.CrearSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.UuidFieldValidationHelper;

public final class CrearSesionRequestValidator implements Validator<CrearSesionRequest> {

    @Override
    public ValidationResult validate(final CrearSesionRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "La informacion de la sesion es obligatoria.").build();
        }
        UuidFieldValidationHelper.validateRequiredUuid(
                builder,
                "grupo",
                request.getGrupo(),
                "El grupo es obligatorio.",
                "El identificador del grupo no es valido."
        );
        validateRequiredLength(builder, "tema", request.getTema(), 5, 100, "El tema es obligatorio.", "El tema debe tener entre 5 y 100 caracteres.");
        validateOptionalMaxLength(builder, "descripcion", request.getDescripcion(), 250, "La descripcion puede tener maximo 250 caracteres.");
        return builder.build();
    }

    private static void validateRequiredLength(
            final ValidationResultBuilder builder,
            final String field,
            final String value,
            final int min,
            final int max,
            final String requiredMessage,
            final String lengthMessage
    ) {
        if (!ValidationHelper.hasText(value)) {
            builder.add(field, ValidationErrorType.REQUIRED, requiredMessage);
            return;
        }
        builder.addIf(
                !ValidationHelper.isLengthBetween(value.trim(), min, max),
                field,
                ValidationErrorType.INVALID_LENGTH,
                lengthMessage
        );
    }

    private static void validateOptionalMaxLength(
            final ValidationResultBuilder builder,
            final String field,
            final String value,
            final int max,
            final String lengthMessage
    ) {
        if (ValidationHelper.hasText(value)) {
            builder.addIf(
                    !ValidationHelper.isLengthAtMost(value.trim(), max),
                    field,
                    ValidationErrorType.INVALID_LENGTH,
                    lengthMessage
            );
        }
    }
}
