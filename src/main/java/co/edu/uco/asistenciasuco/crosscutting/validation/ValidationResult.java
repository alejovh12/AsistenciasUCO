package co.edu.uco.asistenciasuco.crosscutting.validation;

import java.util.List;

public final class ValidationResult {

    private static final ValidationResult VALID = new ValidationResult(List.of());

    private final List<ValidationIssue> issues;

    private ValidationResult(final List<ValidationIssue> issues) {
        this.issues = List.copyOf(issues == null ? List.of() : issues);
    }

    public static ValidationResult valid() {
        return VALID;
    }

    public static ValidationResult of(final List<ValidationIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return valid();
        }
        return new ValidationResult(issues);
    }

    public boolean isValid() {
        return issues.isEmpty();
    }

    public boolean hasErrors() {
        return !isValid();
    }

    public List<ValidationIssue> issues() {
        return issues;
    }
}
