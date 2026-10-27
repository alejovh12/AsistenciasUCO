package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationHelper;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.request.AccesoCuentaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.HttpValidationMessages;

public final class AccesoCuentaRequestValidator implements Validator<AccesoCuentaRequest> {

    private static final int MAX_EMAIL_LENGTH = 100;

    @Override
    public ValidationResult validate(final AccesoCuentaRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "La informacion de la cuenta es obligatoria.").build();
        }
        if (!ValidationHelper.hasText(request.getCorreo())) {
            return builder.add("correo", ValidationErrorType.REQUIRED, "El correo es obligatorio.").build();
        }
        final String normalized = request.getCorreo().trim();
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
        return builder.build();
    }
}
