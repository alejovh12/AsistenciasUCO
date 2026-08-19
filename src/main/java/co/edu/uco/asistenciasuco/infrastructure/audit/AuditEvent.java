package co.edu.uco.asistenciasuco.infrastructure.audit;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditEvent(
        UUID id,
        OffsetDateTime occurredAt,
        String actorId,
        AuditActorType actorType,
        String action,
        String resourceType,
        String resourceId,
        String correlationId,
        String traceId,
        String spanId,
        String httpMethod,
        String path,
        Integer httpStatus,
        String clientIp,
        String errorCode,
        AuditOutcome outcome,
        Map<String, String> metadata
) {
}
