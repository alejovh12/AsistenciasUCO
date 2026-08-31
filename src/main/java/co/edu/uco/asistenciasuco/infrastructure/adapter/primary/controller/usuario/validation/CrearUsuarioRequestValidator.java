package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.validation;

import co.edu.uco.asistenciasuco.crosscutting.helpers.validation.ValidationHelper;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.request.CrearUsuarioRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.HttpValidationMessages;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.UuidFieldValidationHelper;

public final class CrearUsuarioRequestValidator implements Validator<CrearUsuarioRequest> {

    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_EMAIL_LENGTH = 100;
    private static final int MIN_IDENTIFICATION_DIGITS = 6;
    private static final int MAX_IDENTIFICATION_DIGITS = 10;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 255;

    @Override
    public ValidationResult validate(final CrearUsuarioRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add(
                    "request",
                    ValidationErrorType.REQUIRED,
                    "La informacion del usuario es obligatoria."
            ).build();
        }

        UuidFieldValidationHelper.validateRequiredUuid(
                builder,
                "tipoIdIdentificacion",
                request.getTipoIdIdentificacion(),
                "El tipo de identificacion es obligatorio.",
                "El tipo de identificacion no es valido."
        );
        validateNumeroIdentificacion(builder, request.getNumeroIdentificacion());
        validateRequiredPersonName(builder, "primerNombre", request.getPrimerNombre(), "El primer nombre es obligatorio.", "El primer nombre puede tener maximo 50 caracteres.");
        validateRequiredPersonName(builder, "primerApellido", request.getPrimerApellido(), "El primer apellido es obligatorio.", "El primer apellido puede tener maximo 50 caracteres.");
        validateOptionalPersonName(builder, "segundoNombre", request.getSegundoNombre(), "El segundo nombre puede tener maximo 50 caracteres.");
        validateOptionalPersonName(builder, "segundoApellido", request.getSegundoApellido(), "El segundo apellido puede tener maximo 50 caracteres.");
        validateCorreo(builder, request.getCorreo());
        validatePassword(builder, request.getPassword());
        return builder.build();
    }

    private static void validateNumeroIdentificacion(final ValidationResultBuilder builder, final Integer value) {
        if (value == null) {
            builder.add("numeroIdentificacion", ValidationErrorType.REQUIRED, "El numero de identificacion es obligatorio.");
        } else if (!ValidationHelper.isPositive(value)) {
            builder.add(
                    "numeroIdentificacion",
                    ValidationErrorType.OUT_OF_RANGE,
                    HttpValidationMessages.INVALID_IDENTIFICATION_NUMBER
            );
        } else if (!ValidationHelper.hasDigitCount(value, MIN_IDENTIFICATION_DIGITS, MAX_IDENTIFICATION_DIGITS)) {
            builder.add(
                    "numeroIdentificacion",
                    ValidationErrorType.OUT_OF_RANGE,
                    "El numero de identificacion debe contener entre 6 y 10 digitos."
            );
        }
    }

    private static void validateRequiredPersonName(
            final ValidationResultBuilder builder,
            final String field,
            final String value,
            final String requiredMessage,
            final String lengthMessage
    ) {
        if (!ValidationHelper.hasText(value)) {
            builder.add(field, ValidationErrorType.REQUIRED, requiredMessage);
            return;
        }
        validatePersonName(builder, field, value, lengthMessage);
    }

    private static void validateOptionalPersonName(
            final ValidationResultBuilder builder,
            final String field,
            final String value,
            final String lengthMessage
    ) {
        if (ValidationHelper.hasText(value)) {
            validatePersonName(builder, field, value, lengthMessage);
        }
    }

    private static void validatePersonName(
            final ValidationResultBuilder builder,
            final String field,
            final String value,
            final String lengthMessage
    ) {
        final String normalized = ValidationHelper.normalizeSpaces(value);
        builder.addIf(
                !ValidationHelper.isLengthAtMost(normalized, MAX_NAME_LENGTH),
                field,
                ValidationErrorType.INVALID_LENGTH,
                lengthMessage
        );
        builder.addIf(
                !ValidationHelper.isValidPersonName(normalized),
                field,
                ValidationErrorType.INVALID_FORMAT,
                HttpValidationMessages.INVALID_PERSON_NAME
        );
    }

    private static void validateCorreo(final ValidationResultBuilder builder, final String value) {
        if (!ValidationHelper.hasText(value)) {
            builder.add("correo", ValidationErrorType.REQUIRED, "El correo es obligatorio.");
            return;
        }
        final String normalized = value.trim();
        builder.addIf(
                !ValidationHelper.isLengthAtMost(normalized, MAX_EMAIL_LENGTH),
                "correo",
                ValidationErrorType.INVALID_LENGTH,
                "El correo puede tener maximo 100 caracteres."
        );
        builder.addIf(
                !ValidationHelper.isValidEmail(normalized),
                "correo",
                ValidationErrorType.INVALID_FORMAT,
                HttpValidationMessages.INVALID_EMAIL
        );
    }

    private static void validatePassword(final ValidationResultBuilder builder, final String value) {
        if (!ValidationHelper.hasText(value)) {
            builder.add("password", ValidationErrorType.REQUIRED, "La clave es obligatoria.");
            return;
        }
        final int length = value.length();
        builder.addIf(
                length < MIN_PASSWORD_LENGTH || length > MAX_PASSWORD_LENGTH,
                "password",
                ValidationErrorType.INVALID_LENGTH,
                "La clave debe tener entre 8 y 255 caracteres."
        );
    }
}
