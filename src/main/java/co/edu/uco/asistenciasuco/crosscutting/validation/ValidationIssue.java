package co.edu.uco.asistenciasuco.crosscutting.validation;

public record ValidationIssue(
        String field,
        ValidationErrorType type,
        String message
) {
}
