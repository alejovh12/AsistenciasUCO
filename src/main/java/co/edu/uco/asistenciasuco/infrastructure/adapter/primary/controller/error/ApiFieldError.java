package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

public record ApiFieldError(
        String field,
        String code,
        String message
) {
}
