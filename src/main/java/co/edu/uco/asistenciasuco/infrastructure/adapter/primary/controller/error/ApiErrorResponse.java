package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String correlationId,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<ApiFieldError> details
) {
}
