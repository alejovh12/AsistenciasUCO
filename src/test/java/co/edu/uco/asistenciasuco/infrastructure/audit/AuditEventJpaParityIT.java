package co.edu.uco.asistenciasuco.infrastructure.audit;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.AuditEventJpaRepository;
import co.edu.uco.asistenciasuco.infrastructure.audit.adapter.logging.LoggingAuditEventPublisher;
import co.edu.uco.asistenciasuco.infrastructure.audit.adapter.sqlserver.AuditEventJdbcBaselineOracle;
import co.edu.uco.asistenciasuco.infrastructure.audit.model.AuditActorType;
import co.edu.uco.asistenciasuco.infrastructure.audit.model.AuditEvent;
import co.edu.uco.asistenciasuco.infrastructure.audit.model.AuditOutcome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * LB-008 JPA-06B — paridad REAL de auditoria sobre SQL Server: INSERT JDBC (oraculo, solo src/test) frente a
 * {@link AuditEventJpaRepository}, lectura {@code findLatestByCorrelationId}, transaccion {@code REQUIRES_NEW} y
 * fail-open del publicador. Limpia sus filas en {@code @AfterEach}.
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class AuditEventJpaParityIT {

    private static final String MODULO = "AUDIT_PARITY_IT";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuditEventJpaRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final List<UUID> creados = new ArrayList<>();

    @AfterEach
    void limpiar() {
        for (UUID id : creados) {
            jdbcTemplate.update("DELETE FROM dbo.AuditoriaEvento WHERE id = ?", id.toString());
        }
        creados.clear();
    }

    @Test
    void insert_jpa_produce_las_mismas_columnas_que_el_insert_jdbc_baseline() {
        final OffsetDateTime cuando = OffsetDateTime.of(2026, 9, 14, 10, 0, 0, 0, ZoneOffset.UTC);
        final AuditEvent jdbcEvento = evento(cuando, UUID.randomUUID(), Map.of("handlerType", "HTTP"));
        final AuditEvent jpaEvento = evento(cuando, UUID.randomUUID(), Map.of("handlerType", "HTTP"));

        oraculoJdbc().insert(jdbcEvento);
        repository.insert(jpaEvento);
        creados.add(jdbcEvento.id());
        creados.add(jpaEvento.id());

        final Map<String, Object> filaJdbc = filaPorId(jdbcEvento.id());
        final Map<String, Object> filaJpa = filaPorId(jpaEvento.id());
        final List<String> columnas = List.of("occurredAt", "actorId", "actorType", "action", "resourceType",
                "resourceId", "result", "traceId", "spanId", "httpMethod", "path", "httpStatus", "clientIp",
                "errorCode", "metadata");
        for (String columna : columnas) {
            assertEquals(String.valueOf(filaJdbc.get(columna)), String.valueOf(filaJpa.get(columna)),
                    "Columna " + columna + " debe coincidir entre JDBC y JPA");
        }
        assertEquals(jdbcEvento.correlationId(), String.valueOf(filaJdbc.get("correlationId")).toLowerCase());
        assertEquals(jpaEvento.correlationId(), String.valueOf(filaJpa.get("correlationId")).toLowerCase());
        assertTrue(String.valueOf(filaJpa.get("occurredAt")).endsWith("+00:00"), "occurredAt debe conservar offset UTC");
    }

    @Test
    void findLatest_jpa_coincide_con_la_lectura_jdbc_baseline_y_ordena_por_occurredAt() {
        final UUID correlacion = UUID.randomUUID();
        final AuditEvent viejo = evento(OffsetDateTime.of(2026, 9, 14, 9, 0, 0, 0, ZoneOffset.UTC), correlacion, Map.of());
        final AuditEvent nuevo = evento(OffsetDateTime.of(2026, 9, 14, 11, 0, 0, 0, ZoneOffset.UTC), correlacion,
                Map.of("retry", "2"));
        repository.insert(viejo);
        repository.insert(nuevo);
        creados.add(viejo.id());
        creados.add(nuevo.id());

        final Optional<AuditEvent> jpa = repository.findLatestByCorrelationId(correlacion.toString());
        final Optional<AuditEvent> jdbc = oraculoJdbc().findLatestByCorrelationId(correlacion.toString());

        assertTrue(jpa.isPresent());
        assertEquals(nuevo.id(), jpa.orElseThrow().id(), "El ultimo evento por occurredAt debe ganar");
        assertEquals(jdbc, jpa, "Lectura JPA y JDBC baseline deben reconstruir el mismo evento");
        assertFalse(repository.findLatestByCorrelationId(UUID.randomUUID().toString()).isPresent());
    }

    @Test
    void REQUIRES_NEW_persiste_aunque_la_transaccion_de_negocio_haga_rollback() {
        final AuditEvent evento = evento(OffsetDateTime.now(ZoneOffset.UTC), UUID.randomUUID(), Map.of());
        final TransactionTemplate tx = new TransactionTemplate(transactionManager);

        tx.executeWithoutResult(status -> {
            repository.insert(evento);
            status.setRollbackOnly();
        });
        creados.add(evento.id());

        assertEquals(1L, contarPorId(evento.id()), "La auditoria debe commitear en su propia transaccion");
    }

    @Test
    void fallo_de_persistencia_no_se_propaga_al_publicador() {
        final AuditEvent invalido = new AuditEvent(UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC), "u",
                AuditActorType.USER, "CREATE", "Sesion", "r", "no-es-uuid", null, null, "POST", "/p", 200,
                "127.0.0.1", null, AuditOutcome.SUCCESS, Map.of());
        final LoggingAuditEventPublisher publicador = new LoggingAuditEventPublisher(proveedor(repository));

        publicador.publish(invalido);

        assertEquals(0L, contarPorId(invalido.id()), "El fallo se registra y no escribe fila");
    }

    private AuditEventJdbcBaselineOracle oraculoJdbc() {
        return new AuditEventJdbcBaselineOracle(proveedor(jdbcTemplate));
    }

    private static <T> ObjectProvider<T> proveedor(final T bean) {
        @SuppressWarnings("unchecked")
        final ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(bean);
        return provider;
    }

    private Map<String, Object> filaPorId(final UUID id) {
        return jdbcTemplate.queryForMap("SELECT CAST(occurredAt AS NVARCHAR(40)) AS occurredAt, "
                + "CAST(correlationId AS NVARCHAR(40)) AS correlationId, actorId, actorType, action, resourceType, "
                + "resourceId, result, traceId, spanId, httpMethod, path, httpStatus, clientIp, errorCode, metadata "
                + "FROM dbo.AuditoriaEvento WHERE id = ?", id.toString());
    }

    private long contarPorId(final UUID id) {
        return jdbcTemplate.queryForObject("SELECT COUNT_BIG(*) FROM dbo.AuditoriaEvento WHERE id = ?", Long.class,
                id.toString());
    }

    private static AuditEvent evento(final OffsetDateTime cuando, final UUID correlacion, final Map<String, String> metadata) {
        return new AuditEvent(UUID.randomUUID(), cuando, "usuario-" + MODULO, AuditActorType.USER, "CREATE",
                "Sesion", "RECURSO-" + MODULO, correlacion.toString(), null, null, "POST",
                "/api/v1/sesiones", 201, "127.0.0.1", null, AuditOutcome.SUCCESS, metadata);
    }
}
