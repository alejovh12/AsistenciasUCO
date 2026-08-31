package co.edu.uco.asistenciasuco.crosscutting.validation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ValidationResultBuilder {

    private final List<ValidationIssue> issues = new ArrayList<>();

    public ValidationResultBuilder add(
            final String field,
            final ValidationErrorType type,
            final String message
    ) {
        issues.add(new ValidationIssue(
                Objects.requireNonNull(field, "El campo del error de validacion es obligatorio."),
                Objects.requireNonNull(type, "El tipo del error de validacion es obligatorio."),
                Objects.requireNonNull(message, "El mensaje del error de validacion es obligatorio.")
        ));
        return this;
    }

    public ValidationResultBuilder addIf(
            final boolean condition,
            final String field,
            final ValidationErrorType type,
            final String message
    ) {
        if (condition) {
            add(field, type, message);
        }
        return this;
    }

    public ValidationResult build() {
        issues.sort(Comparator.comparing(ValidationIssue::field).thenComparing(ValidationIssue::type));
        return ValidationResult.of(issues);
    }
}
