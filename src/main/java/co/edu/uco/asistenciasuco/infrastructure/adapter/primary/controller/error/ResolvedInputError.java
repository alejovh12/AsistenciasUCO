package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

public record ResolvedInputError(
        String field,
        ApiFieldErrorCode code,
        String message
) {
}
