package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.RequestValidationException;

import java.util.Objects;

public final class RequestValidationGuard {

    private RequestValidationGuard() {
    }

    public static void validate(final ValidationResult validationResult) {
        validate(validationResult, null);
    }

    /** Como {@link #validate(ValidationResult)} pero el 400 lleva el codigo de catalogo indicado (p. ej. {@code PAG_001}). */
    public static void validate(final ValidationResult validationResult, final String errorCode) {
        Objects.requireNonNull(validationResult, "El resultado de validacion es obligatorio.");
        if (validationResult.hasErrors()) {
            throw new RequestValidationException(validationResult, errorCode);
        }
    }
}
