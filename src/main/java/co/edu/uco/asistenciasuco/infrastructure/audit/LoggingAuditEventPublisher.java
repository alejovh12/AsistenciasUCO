package co.edu.uco.asistenciasuco.infrastructure.audit;

import co.edu.uco.asistenciasuco.crosscutting.sanitization.SensitiveDataSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class LoggingAuditEventPublisher implements AuditEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAuditEventPublisher.class);
    private final AuditEventJdbcRepository repository;

    public LoggingAuditEventPublisher(final ObjectProvider<AuditEventJdbcRepository> repositoryProvider) {
        this.repository = repositoryProvider.getIfAvailable();
    }

    @Override
    public void publish(final AuditEvent event) {
        if (repository != null) {
            try {
                repository.insert(event);
            } catch (Exception exception) {
                LOGGER.atError()
                        .setCause(exception)
                        .addKeyValue("eventType", "AUDIT_PERSISTENCE_FAILED")
                        .addKeyValue("auditId", event.id())
                        .addKeyValue("action", event.action())
                        .addKeyValue("resourceType", event.resourceType())
                        .addKeyValue("resourceId", event.resourceId())
                        .addKeyValue("correlationId", event.correlationId())
                        .addKeyValue("traceId", event.traceId())
                        .log("Audit persistence failed.");
                return;
            }
        } else {
            LOGGER.warn("Audit repository is not available. Skipping durable persistence for current context.");
            return;
        }
        LOGGER.atInfo()
                .addKeyValue("eventType", "AUDIT")
                .addKeyValue("auditId", event.id())
                .addKeyValue("actorId", safe(event.actorId(), 120))
                .addKeyValue("actorType", event.actorType())
                .addKeyValue("action", safe(event.action(), 120))
                .addKeyValue("resourceType", safe(event.resourceType(), 120))
                .addKeyValue("resourceId", safe(event.resourceId(), 120))
                .addKeyValue("occurredAt", event.occurredAt())
                .addKeyValue("httpMethod", safe(event.httpMethod(), 16))
                .addKeyValue("path", safe(event.path(), 240))
                .addKeyValue("httpStatus", event.httpStatus())
                .addKeyValue("clientIp", safe(event.clientIp(), 80))
                .addKeyValue("errorCode", safe(event.errorCode(), 120))
                .addKeyValue("result", event.outcome())
                .addKeyValue("metadata", SensitiveDataSanitizer.sanitizeMetadata(event.metadata()))
                .log("Audit event persisted.");
    }

    private String safe(final String value, final int maxLength) {
        return SensitiveDataSanitizer.sanitizeForLog(value, maxLength);
    }
}
