package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;

import java.util.Objects;

public final class RequestValidationException extends RuntimeException {

    private final ValidationResult validationResult;

    public RequestValidationException(final ValidationResult validationResult) {
        super("HTTP request validation failed.");
        this.validationResult = Objects.requireNonNull(validationResult, "El resultado de validacion es obligatorio.");
        if (validationResult.isValid()) {
            throw new IllegalArgumentException("No se puede crear una excepcion de validacion HTTP con un resultado valido.");
        }
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }
}
