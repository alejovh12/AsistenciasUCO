package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.sanitization.SensitiveDataSanitizer;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.AuditEventEntity;
import co.edu.uco.asistenciasuco.infrastructure.audit.model.AuditActorType;
import co.edu.uco.asistenciasuco.infrastructure.audit.model.AuditEvent;
import co.edu.uco.asistenciasuco.infrastructure.audit.model.AuditOutcome;
import jakarta.persistence.EntityManager;
import org.springframework.boot.json.JsonParseException;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.json.JsonWriter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA de {@code dbo.AuditoriaEvento} (LB-008 JPA-06B, TD-010 OPTION A: DML directo sin SP).
 *
 * <p>La insercion es independiente de la transaccion de negocio ({@code REQUIRES_NEW}): replica el autocommit
 * del baseline JDBC. La sanitizacion y los limites de longitud son responsabilidad de esta clase, no de la DB.</p>
 */
@Repository
public class AuditEventJpaRepository {

    static final String JPQL_FIND_LATEST_BY_CORRELATION_ID = """
            select a
            from AuditEventEntity a
            where a.correlationId = :correlationId
            order by a.occurredAt desc, a.id desc
            """;

    private final EntityManager entityManager;
    private final JsonParser jsonParser;

    public AuditEventJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de auditoria es obligatorio.");
        this.jsonParser = JsonParserFactory.getJsonParser();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insert(final AuditEvent event) {
        if (ObjectHelper.isNull(event)) {
            throw new CrosscuttingException("El evento de auditoria es obligatorio.");
        }
        entityManager.persist(toEntity(event));
    }

    public Optional<AuditEvent> findLatestByCorrelationId(final String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return Optional.empty();
        }
        final List<AuditEventEntity> result = entityManager
                .createQuery(JPQL_FIND_LATEST_BY_CORRELATION_ID, AuditEventEntity.class)
                .setParameter("correlationId", UUID.fromString(correlationId))
                .setMaxResults(1)
                .getResultList();
        return result.stream().findFirst().map(this::toDomain);
    }

    private AuditEventEntity toEntity(final AuditEvent event) {
        return new AuditEventEntity(
                event.id(),
                event.occurredAt(),
                safe(event.actorId(), 120),
                event.actorType() == null ? null : event.actorType().name(),
                safe(event.action(), 120),
                safe(event.resourceType(), 120),
                safe(event.resourceId(), 120),
                event.outcome() == null ? null : event.outcome().name(),
                event.correlationId() == null ? null : UUID.fromString(safe(event.correlationId(), 36)),
                safe(event.traceId(), 32),
                safe(event.spanId(), 16),
                safe(event.httpMethod(), 16),
                safe(event.path(), 240),
                event.httpStatus(),
                safe(event.clientIp(), 45),
                safe(event.errorCode(), 120),
                metadataJson(event.metadata())
        );
    }

    private AuditEvent toDomain(final AuditEventEntity entity) {
        return new AuditEvent(
                entity.getId(),
                entity.getOccurredAt(),
                entity.getActorId(),
                toActorType(entity.getActorType()),
                entity.getAction(),
                entity.getResourceType(),
                normalizedUuidString(entity.getResourceId()),
                entity.getCorrelationId() == null ? null : entity.getCorrelationId().toString(),
                entity.getTraceId(),
                entity.getSpanId(),
                entity.getHttpMethod(),
                entity.getPath(),
                entity.getHttpStatus(),
                entity.getClientIp(),
                entity.getErrorCode(),
                toOutcome(entity.getResult()),
                metadataFromJson(entity.getMetadata())
        );
    }

    private String metadataJson(final Map<String, String> metadata) {
        final Map<String, String> safeMetadata = new LinkedHashMap<>(SensitiveDataSanitizer.sanitizeMetadata(metadata));
        if (safeMetadata.isEmpty()) {
            return null;
        }
        return JsonWriter.<Map<String, String>>standard().writeToString(safeMetadata);
    }

    private Map<String, String> metadataFromJson(final String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            final Map<String, Object> values = jsonParser.parseMap(metadataJson);
            final Map<String, String> metadata = new LinkedHashMap<>();
            values.forEach((key, value) -> metadata.put(key, value == null ? null : String.valueOf(value)));
            return metadata;
        } catch (JsonParseException exception) {
            throw new CrosscuttingException("No fue posible leer la metadata de auditoria almacenada.", exception);
        }
    }

    private String normalizedUuidString(final String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        try {
            return UUID.fromString(value).toString();
        } catch (IllegalArgumentException exception) {
            return value;
        }
    }

    private static AuditActorType toActorType(final String value) {
        return value == null || value.isBlank() ? null : AuditActorType.valueOf(value);
    }

    private static AuditOutcome toOutcome(final String value) {
        return value == null || value.isBlank() ? null : AuditOutcome.valueOf(value);
    }

    private static String safe(final String value, final int maxLength) {
        return SensitiveDataSanitizer.sanitizeForLog(value, maxLength);
    }
}
