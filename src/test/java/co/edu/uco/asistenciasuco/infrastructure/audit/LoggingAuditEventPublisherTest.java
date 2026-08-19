package co.edu.uco.asistenciasuco.infrastructure.audit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoggingAuditEventPublisherTest {

    @Test
    void publish_persistencia_exitosa_registra_eventType_audit() {
        final AuditEventJdbcRepository repository = mock(AuditEventJdbcRepository.class);
        final LoggingAuditEventPublisher publisher = new LoggingAuditEventPublisher(provider(repository));
        final AuditEvent event = event();
        final ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(LoggingAuditEventPublisher.class);
        final ListAppender<ILoggingEvent> appender = appender(logger);

        try {
            publisher.publish(event);

            verify(repository).insert(event);
            final ILoggingEvent loggingEvent = lastEvent(appender);
            assertEquals(Level.INFO, loggingEvent.getLevel());
            assertEquals("Audit event persisted.", loggingEvent.getFormattedMessage());
            assertEquals("AUDIT", keyValues(loggingEvent).get("eventType"));
        } finally {
            detach(logger, appender);
        }
    }

    @Test
    void publish_fallo_persistencia_registra_error_seguro_y_no_propaga() {
        final AuditEventJdbcRepository repository = mock(AuditEventJdbcRepository.class);
        final AuditEvent event = event();
        final RuntimeException failure = new RuntimeException("db unavailable");
        doThrow(failure).when(repository).insert(event);
        final LoggingAuditEventPublisher publisher = new LoggingAuditEventPublisher(provider(repository));
        final ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(LoggingAuditEventPublisher.class);
        final ListAppender<ILoggingEvent> appender = appender(logger);

        try {
            publisher.publish(event);

            final ILoggingEvent loggingEvent = lastEvent(appender);
            final Map<String, String> keyValues = keyValues(loggingEvent);
            assertEquals(Level.ERROR, loggingEvent.getLevel());
            assertEquals("Audit persistence failed.", loggingEvent.getFormattedMessage());
            assertEquals("AUDIT_PERSISTENCE_FAILED", keyValues.get("eventType"));
            assertEquals(event.id().toString(), keyValues.get("auditId"));
            assertEquals(event.action(), keyValues.get("action"));
            assertEquals(event.resourceType(), keyValues.get("resourceType"));
            assertEquals(event.resourceId(), keyValues.get("resourceId"));
            assertEquals(event.correlationId(), keyValues.get("correlationId"));
            assertEquals(event.traceId(), keyValues.get("traceId"));
            assertNull(keyValues.get("actorId"));
            assertNull(keyValues.get("metadata"));
            assertNotNull(loggingEvent.getThrowableProxy());
            assertTrue(appender.list.stream().noneMatch(log -> "Audit event persisted.".equals(log.getFormattedMessage())));
        } finally {
            detach(logger, appender);
        }
    }

    private ObjectProvider<AuditEventJdbcRepository> provider(final AuditEventJdbcRepository repository) {
        final ObjectProvider<AuditEventJdbcRepository> provider = mock();
        when(provider.getIfAvailable()).thenReturn(repository);
        return provider;
    }

    private AuditEvent event() {
        return new AuditEvent(
                UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c"),
                OffsetDateTime.parse("2026-08-19T10:15:30-05:00"),
                "actor@uco.edu",
                AuditActorType.USER,
                "SOLICITAR_REVISION_ASISTENCIA",
                "ASISTENCIA",
                "00000000-0000-0000-0000-000000000101",
                "corr-123",
                "trace-123",
                "span-123",
                "POST",
                "/api/v1/asistencias/revisiones",
                202,
                "127.0.0.1",
                null,
                AuditOutcome.SUCCESS,
                Map.of("handlerType", "HTTP")
        );
    }

    private ListAppender<ILoggingEvent> appender(final ch.qos.logback.classic.Logger logger) {
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void detach(final ch.qos.logback.classic.Logger logger, final ListAppender<ILoggingEvent> appender) {
        logger.detachAppender(appender);
        appender.stop();
    }

    private ILoggingEvent lastEvent(final ListAppender<ILoggingEvent> appender) {
        return appender.list.stream().reduce((first, second) -> second).orElseThrow();
    }

    private Map<String, String> keyValues(final ILoggingEvent event) {
        return event.getKeyValuePairs().stream()
                .collect(Collectors.toMap(keyValue -> keyValue.key, keyValue -> String.valueOf(keyValue.value), (left, right) -> right));
    }
}
