package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.RequestValidationException;

import java.util.Objects;

public final class RequestValidationGuard {

    private RequestValidationGuard() {
    }

    public static void validate(final ValidationResult validationResult) {
        Objects.requireNonNull(validationResult, "El resultado de validacion es obligatorio.");
        if (validationResult.hasErrors()) {
            throw new RequestValidationException(validationResult);
        }
    }
}
