package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.audit;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.observability.audit.AuditActorType;
import co.edu.uco.asistenciasuco.infrastructure.observability.audit.AuditEvent;
import co.edu.uco.asistenciasuco.infrastructure.observability.audit.AuditOutcome;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlProvider;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuditEventJdbcRepositoryTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CORRELATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-09-14T10:00:00Z");

    @Test
    void metadataFromJson_reconstruye_metadata_persistida() {
        final AuditEventJdbcRepository repository = repository(null);

        assertEquals(Map.of("handlerType", "HTTP", "retry", "2"),
                repository.metadataFromJson("{\"handlerType\":\"HTTP\",\"retry\":2}"));
        assertTrue(repository.metadataFromJson(null).isEmpty());
        assertTrue(repository.metadataFromJson(" ").isEmpty());
        assertThrows(CrosscuttingException.class, () -> repository.metadataFromJson("{json-invalido"));
    }

    @Test
    void insertRechazaEventoNuloYPersistenciaNoDisponible() {
        final AuditEventJdbcRepository repository = repository(null);

        assertThrows(CrosscuttingException.class, () -> repository.insert(null));
        assertThrows(CrosscuttingException.class, () -> repository.insert(evento()));
    }

    @Test
    void insertUsaTablaYParametrosNombradosDeAuditoria() {
        final JdbcTemplate jdbc = mock(JdbcTemplate.class);
        final AuditEventJdbcRepository repository = repository(jdbc);

        repository.insert(evento());

        final ArgumentCaptor<PreparedStatementCreator> captor = ArgumentCaptor.forClass(PreparedStatementCreator.class);
        verify(jdbc).update(captor.capture());
        final String sql = ((SqlProvider) captor.getValue()).getSql();
        assertTrue(sql.contains("INSERT INTO dbo.AuditoriaEvento"));
        assertTrue(sql.contains("occurredAt"));
        assertTrue(sql.contains("metadata"));
        assertFalse(sql.contains(":id"));
    }

    @Test
    void busquedaSinCorrelationIdNoConsultaJdbc() {
        final JdbcTemplate jdbc = mock(JdbcTemplate.class);
        final AuditEventJdbcRepository repository = repository(jdbc);

        assertTrue(repository.findLatestByCorrelationId(null).isEmpty());
        assertTrue(repository.findLatestByCorrelationId(" ").isEmpty());
        verifyNoInteractions(jdbc);
        assertTrue(repository(null).findLatestByCorrelationId(CORRELATION_ID.toString()).isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void busquedaReconstruyeEventoConUuidYMetadata() throws SQLException {
        final JdbcTemplate jdbc = mock(JdbcTemplate.class);
        final ResultSet row = mock(ResultSet.class);
        final AuditEventJdbcRepository repository = repository(jdbc);
        when(row.getObject("id")).thenReturn(EVENT_ID);
        when(row.getObject("occurredAt", OffsetDateTime.class)).thenReturn(OCCURRED_AT);
        when(row.getString("actorId")).thenReturn("usuario-1");
        when(row.getString("actorType")).thenReturn("USER");
        when(row.getString("action")).thenReturn("CREATE");
        when(row.getString("resourceType")).thenReturn("Sesion");
        when(row.getString("resourceId")).thenReturn(EVENT_ID.toString().toUpperCase());
        when(row.getString("correlationId")).thenReturn(CORRELATION_ID.toString().toUpperCase());
        when(row.getString("httpMethod")).thenReturn("POST");
        when(row.getString("path")).thenReturn("/api/v1/sesiones");
        when(row.getObject("httpStatus", Integer.class)).thenReturn(201);
        when(row.getString("result")).thenReturn("SUCCESS");
        when(row.getString("metadata")).thenReturn("{\"handlerType\":\"HTTP\"}");
        when(jdbc.query(anyString(), any(RowMapper.class), eq(CORRELATION_ID.toString())))
                .thenAnswer(invocation -> List.of(((RowMapper<AuditEvent>) invocation.getArgument(1)).mapRow(row, 0)));

        final AuditEvent result = repository.findLatestByCorrelationId(CORRELATION_ID.toString()).orElseThrow();

        assertEquals(EVENT_ID, result.id());
        assertEquals(OCCURRED_AT, result.occurredAt());
        assertEquals(AuditActorType.USER, result.actorType());
        assertEquals(AuditOutcome.SUCCESS, result.outcome());
        assertEquals(EVENT_ID.toString(), result.resourceId());
        assertEquals(CORRELATION_ID.toString(), result.correlationId());
        assertEquals(Map.of("handlerType", "HTTP"), result.metadata());
        assertEquals(201, result.httpStatus());
    }

    private static AuditEvent evento() {
        return new AuditEvent(EVENT_ID, OCCURRED_AT, "usuario-1", AuditActorType.USER,
                "CREATE", "Sesion", EVENT_ID.toString(), CORRELATION_ID.toString(),
                null, null, "POST", "/api/v1/sesiones", 201, "127.0.0.1", null,
                AuditOutcome.SUCCESS, Map.of("handlerType", "HTTP"));
    }

    @SuppressWarnings("unchecked")
    private static AuditEventJdbcRepository repository(final JdbcTemplate jdbc) {
        final ObjectProvider<JdbcTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(jdbc);
        return new AuditEventJdbcRepository(provider);
    }
}
