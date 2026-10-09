package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Modelo de persistencia ESCRITURA/LECTURA de la tabla {@code dbo.AuditoriaEvento} (LB-008 JPA-06B, TD-010 OPTION A).
 *
 * <p>Entidad plana de tabla, sin relaciones y sin {@code @Immutable}: se inserta desde
 * {@code AuditEventJpaRepository}. Las 17 columnas corresponden 1:1 a la tabla; no se agregan campos.
 * Los valores llegan ya sanitizados y truncados al limite de su columna desde el repository.</p>
 */
@Entity
@Table(name = "AuditoriaEvento", schema = "dbo")
public class AuditEventEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "occurredAt")
    private OffsetDateTime occurredAt;

    @Column(name = "actorId")
    private String actorId;

    @Column(name = "actorType")
    private String actorType;

    @Column(name = "action")
    private String action;

    @Column(name = "resourceType")
    private String resourceType;

    @Column(name = "resourceId")
    private String resourceId;

    @Column(name = "result")
    private String result;

    @Column(name = "correlationId")
    private UUID correlationId;

    @Column(name = "traceId")
    private String traceId;

    @Column(name = "spanId")
    private String spanId;

    @Column(name = "httpMethod")
    private String httpMethod;

    @Column(name = "path")
    private String path;

    @Column(name = "httpStatus")
    private Integer httpStatus;

    @Column(name = "clientIp")
    private String clientIp;

    @Column(name = "errorCode")
    private String errorCode;

    @Column(name = "metadata")
    private String metadata;

    protected AuditEventEntity() {
        // requerido por JPA
    }

    public AuditEventEntity(final UUID id, final OffsetDateTime occurredAt, final String actorId,
                            final String actorType, final String action, final String resourceType,
                            final String resourceId, final String result, final UUID correlationId,
                            final String traceId, final String spanId, final String httpMethod,
                            final String path, final Integer httpStatus, final String clientIp,
                            final String errorCode, final String metadata) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.actorId = actorId;
        this.actorType = actorType;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.result = result;
        this.correlationId = correlationId;
        this.traceId = traceId;
        this.spanId = spanId;
        this.httpMethod = httpMethod;
        this.path = path;
        this.httpStatus = httpStatus;
        this.clientIp = clientIp;
        this.errorCode = errorCode;
        this.metadata = metadata;
    }

    public UUID getId() {
        return id;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getActorId() {
        return actorId;
    }

    public String getActorType() {
        return actorType;
    }

    public String getAction() {
        return action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getResult() {
        return result;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMetadata() {
        return metadata;
    }
}
