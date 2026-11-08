package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;

import java.util.Objects;

public final class RequestValidationException extends RuntimeException {

    private final ValidationResult validationResult;
    private final String errorCode;

    public RequestValidationException(final ValidationResult validationResult) {
        this(validationResult, null);
    }

    /**
     * @param errorCode codigo de catalogo propio de la validacion (p. ej. {@code PAG_001}, CULM-001 D.9-a); {@code null}
     *                  usa el generico {@code VALIDATION_ERROR}
     */
    public RequestValidationException(final ValidationResult validationResult, final String errorCode) {
        super("HTTP request validation failed.");
        this.validationResult = Objects.requireNonNull(validationResult, "El resultado de validacion es obligatorio.");
        this.errorCode = errorCode == null || errorCode.isBlank() ? null : errorCode;
        if (validationResult.isValid()) {
            throw new IllegalArgumentException("No se puede crear una excepcion de validacion HTTP con un resultado valido.");
        }
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }

    /** Codigo de catalogo que sustituye a {@code VALIDATION_ERROR}, o {@code null}. */
    public String getErrorCode() {
        return errorCode;
    }
}
