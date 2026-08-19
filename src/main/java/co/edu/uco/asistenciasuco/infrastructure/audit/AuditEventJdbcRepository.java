package co.edu.uco.asistenciasuco.infrastructure.audit;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.sanitization.SensitiveDataSanitizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.json.JsonParseException;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.json.JsonWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AuditEventJdbcRepository {

    static final String TABLE_NAME = "dbo.AuditoriaEvento";
    private static final String SQL_INSERT = """
            INSERT INTO dbo.AuditoriaEvento (
                id,
                occurredAt,
                actorId,
                actorType,
                action,
                resourceType,
                resourceId,
                result,
                correlationId,
                traceId,
                spanId,
                httpMethod,
                path,
                httpStatus,
                clientIp,
                errorCode,
                metadata
            ) VALUES (
                :id,
                :occurredAt,
                :actorId,
                :actorType,
                :action,
                :resourceType,
                :resourceId,
                :result,
                :correlationId,
                :traceId,
                :spanId,
                :httpMethod,
                :path,
                :httpStatus,
                :clientIp,
                :errorCode,
                :metadata
            )
            """;
    private static final String SQL_FIND_BY_CORRELATION_ID = """
            SELECT TOP 1
                id,
                occurredAt,
                actorId,
                actorType,
                action,
                resourceType,
                resourceId,
                result,
                correlationId,
                traceId,
                spanId,
                httpMethod,
                path,
                httpStatus,
                clientIp,
                errorCode,
                metadata
            FROM dbo.AuditoriaEvento
            WHERE correlationId = ?
            ORDER BY occurredAt DESC, id DESC
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcTemplate queryJdbcTemplate;
    private final JsonParser jsonParser;

    public AuditEventJdbcRepository(final ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        final JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        this.jdbcTemplate = jdbcTemplate == null ? null : new NamedParameterJdbcTemplate(jdbcTemplate);
        this.queryJdbcTemplate = jdbcTemplate;
        this.jsonParser = JsonParserFactory.getJsonParser();
    }

    public void insert(final AuditEvent event) {
        if (ObjectHelper.isNull(event)) {
            throw new CrosscuttingException("El evento de auditoria es obligatorio.");
        }
        if (ObjectHelper.isNull(jdbcTemplate)) {
            throw new CrosscuttingException("La persistencia durable de auditoria no esta disponible en este contexto.");
        }
        jdbcTemplate.update(SQL_INSERT, toParameters(event));
    }

    public Optional<AuditEvent> findLatestByCorrelationId(final String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return Optional.empty();
        }
        if (ObjectHelper.isNull(queryJdbcTemplate)) {
            return Optional.empty();
        }
        final List<AuditEvent> result = queryJdbcTemplate.query(SQL_FIND_BY_CORRELATION_ID, this::mapRow, correlationId);
        return result.stream().findFirst();
    }

    private MapSqlParameterSource toParameters(final AuditEvent event) {
        return new MapSqlParameterSource()
                .addValue("id", event.id().toString(), Types.VARCHAR)
                .addValue("occurredAt", event.occurredAt(), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("actorId", safe(event.actorId(), 120), Types.NVARCHAR)
                .addValue("actorType", event.actorType() == null ? null : event.actorType().name(), Types.NVARCHAR)
                .addValue("action", safe(event.action(), 120), Types.NVARCHAR)
                .addValue("resourceType", safe(event.resourceType(), 120), Types.NVARCHAR)
                .addValue("resourceId", safe(event.resourceId(), 120), Types.NVARCHAR)
                .addValue("result", event.outcome() == null ? null : event.outcome().name(), Types.NVARCHAR)
                .addValue("correlationId", safe(event.correlationId(), 36), Types.NVARCHAR)
                .addValue("traceId", safe(event.traceId(), 32), Types.NVARCHAR)
                .addValue("spanId", safe(event.spanId(), 16), Types.NVARCHAR)
                .addValue("httpMethod", safe(event.httpMethod(), 16), Types.NVARCHAR)
                .addValue("path", safe(event.path(), 240), Types.NVARCHAR)
                .addValue("httpStatus", event.httpStatus(), Types.INTEGER)
                .addValue("clientIp", safe(event.clientIp(), 45), Types.NVARCHAR)
                .addValue("errorCode", safe(event.errorCode(), 120), Types.NVARCHAR)
                .addValue("metadata", metadataJson(event.metadata()), Types.NVARCHAR);
    }

    private String metadataJson(final Map<String, String> metadata) {
        final Map<String, String> safeMetadata = new LinkedHashMap<>(SensitiveDataSanitizer.sanitizeMetadata(metadata));
        if (safeMetadata.isEmpty()) {
            return null;
        }
        return JsonWriter.<Map<String, String>>standard().writeToString(safeMetadata);
    }

    private String safe(final String value, final int maxLength) {
        return SensitiveDataSanitizer.sanitizeForLog(value, maxLength);
    }

    private static UUID toUuid(final Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return value == null ? null : UUID.fromString(String.valueOf(value));
    }

    private static AuditActorType toActorType(final String value) {
        return value == null || value.isBlank() ? null : AuditActorType.valueOf(value);
    }

    private static AuditOutcome toOutcome(final String value) {
        return value == null || value.isBlank() ? null : AuditOutcome.valueOf(value);
    }

    private AuditEvent mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
        return new AuditEvent(
                toUuid(resultSet.getObject("id")),
                resultSet.getObject("occurredAt", OffsetDateTime.class),
                resultSet.getString("actorId"),
                toActorType(resultSet.getString("actorType")),
                resultSet.getString("action"),
                resultSet.getString("resourceType"),
                normalizedUuidString(resultSet.getString("resourceId")),
                normalizedUuidString(resultSet.getString("correlationId")),
                resultSet.getString("traceId"),
                resultSet.getString("spanId"),
                resultSet.getString("httpMethod"),
                resultSet.getString("path"),
                resultSet.getObject("httpStatus", Integer.class),
                resultSet.getString("clientIp"),
                resultSet.getString("errorCode"),
                toOutcome(resultSet.getString("result")),
                metadataFromJson(resultSet.getString("metadata"))
        );
    }

    Map<String, String> metadataFromJson(final String metadataJson) {
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
}
