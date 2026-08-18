package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import java.time.OffsetDateTime;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String correlationId
) {
}
