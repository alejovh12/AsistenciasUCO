package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.AuditEventEntity;
import co.edu.uco.asistenciasuco.infrastructure.audit.model.AuditActorType;
import co.edu.uco.asistenciasuco.infrastructure.audit.model.AuditEvent;
import co.edu.uco.asistenciasuco.infrastructure.audit.model.AuditOutcome;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * LB-008 JPA-06B — unidad del repositorio JPA de auditoria sobre {@code EntityManager} mockeado. Verifica
 * validaciones, mapeo y sanitizacion. La paridad contra SQL Server (INSERT y lectura) vive en el IT.
 */
class AuditEventJpaRepositoryTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CORRELATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-09-14T10:00:00Z");

    @Test
    @SuppressWarnings("unchecked")
    void busquedaReconstruyeMetadataPersistidaOVaciaSegunElJsonAlmacenado() {
        assertEquals(Map.of("handlerType", "HTTP", "retry", "2"),
                latestMetadataFrom("{\"handlerType\":\"HTTP\",\"retry\":\"2\"}"));
        assertTrue(latestMetadataFrom(null).isEmpty());
        assertTrue(latestMetadataFrom(" ").isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void busquedaConMetadataCorruptaFallaConErrorDeCrosscutting() {
        assertThrows(CrosscuttingException.class, () -> latestMetadataFrom("{json-invalido"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> latestMetadataFrom(final String storedMetadata) {
        final EntityManager entityManager = mock(EntityManager.class);
        final TypedQuery<AuditEventEntity> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(AuditEventEntity.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(new AuditEventEntity(EVENT_ID, OCCURRED_AT, "usuario-1",
                "USER", "CREATE", "Sesion", EVENT_ID.toString(), "SUCCESS", CORRELATION_ID, null,
                null, "POST", "/api/v1/sesiones", 201, "127.0.0.1", null, storedMetadata)));

        return new AuditEventJpaRepository(entityManager)
                .findLatestByCorrelationId(CORRELATION_ID.toString())
                .orElseThrow()
                .metadata();
    }

    @Test
    void insertRechazaEventoNulo() {
        final EntityManager entityManager = mock(EntityManager.class);
        final AuditEventJpaRepository repository = new AuditEventJpaRepository(entityManager);

        assertThrows(CrosscuttingException.class, () -> repository.insert(null));
        verifyNoInteractions(entityManager);
    }

    @Test
    void insertPersisteEntidadPlanaConColumnasDeAuditoria() {
        final EntityManager entityManager = mock(EntityManager.class);
        new AuditEventJpaRepository(entityManager).insert(evento(Map.of("handlerType", "HTTP")));

        final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(entityManager).persist(captor.capture());
        final AuditEventEntity entity = (AuditEventEntity) captor.getValue();
        assertEquals(EVENT_ID, entity.getId());
        assertEquals(OCCURRED_AT, entity.getOccurredAt());
        assertEquals("USER", entity.getActorType());
        assertEquals("SUCCESS", entity.getResult());
        assertEquals(CORRELATION_ID, entity.getCorrelationId());
        assertEquals(201, entity.getHttpStatus());
        assertEquals("{\"handlerType\":\"HTTP\"}", entity.getMetadata());
    }

    @Test
    void metadataVaciaSePersisteComoNull() {
        final EntityManager entityManager = mock(EntityManager.class);
        new AuditEventJpaRepository(entityManager).insert(evento(Map.of()));

        final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(entityManager).persist(captor.capture());
        assertNull(((AuditEventEntity) captor.getValue()).getMetadata());
    }

    @Test
    void correlationIdNoUuidFallaAntesDePersistir() {
        final EntityManager entityManager = mock(EntityManager.class);
        final AuditEvent invalido = new AuditEvent(EVENT_ID, OCCURRED_AT, "usuario-1", AuditActorType.USER,
                "CREATE", "Sesion", EVENT_ID.toString(), "no-es-uuid", null, null, "POST", "/api/v1/sesiones",
                201, "127.0.0.1", null, AuditOutcome.SUCCESS, Map.of());

        assertThrows(IllegalArgumentException.class, () -> new AuditEventJpaRepository(entityManager).insert(invalido));
        verify(entityManager, never()).persist(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void busquedaSinCorrelationIdNoConsultaEntityManager() {
        final EntityManager entityManager = mock(EntityManager.class);
        final AuditEventJpaRepository repository = new AuditEventJpaRepository(entityManager);

        assertTrue(repository.findLatestByCorrelationId(null).isEmpty());
        assertTrue(repository.findLatestByCorrelationId(" ").isEmpty());
        verifyNoInteractions(entityManager);
    }

    @Test
    @SuppressWarnings("unchecked")
    void busquedaUsaJpqlOrdenadaConLimiteUnoYReconstruyeEvento() {
        final EntityManager entityManager = mock(EntityManager.class);
        final TypedQuery<AuditEventEntity> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(AuditEventEntity.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(new AuditEventEntity(EVENT_ID, OCCURRED_AT, "usuario-1",
                "USER", "CREATE", "Sesion", EVENT_ID.toString().toUpperCase(), "SUCCESS", CORRELATION_ID, null,
                null, "POST", "/api/v1/sesiones", 201, "127.0.0.1", null, "{\"handlerType\":\"HTTP\"}")));

        final Optional<AuditEvent> result = new AuditEventJpaRepository(entityManager)
                .findLatestByCorrelationId(CORRELATION_ID.toString());

        verify(query).setParameter("correlationId", CORRELATION_ID);
        verify(query).setMaxResults(1);
        assertEquals(EVENT_ID.toString(), result.orElseThrow().resourceId());
        assertEquals(AuditActorType.USER, result.orElseThrow().actorType());
        assertEquals(AuditOutcome.SUCCESS, result.orElseThrow().outcome());
        assertEquals(Map.of("handlerType", "HTTP"), result.orElseThrow().metadata());
    }

    private static AuditEvent evento(final Map<String, String> metadata) {
        return new AuditEvent(EVENT_ID, OCCURRED_AT, "usuario-1", AuditActorType.USER,
                "CREATE", "Sesion", EVENT_ID.toString(), CORRELATION_ID.toString(),
                null, null, "POST", "/api/v1/sesiones", 201, "127.0.0.1", null,
                AuditOutcome.SUCCESS, metadata);
    }
}
