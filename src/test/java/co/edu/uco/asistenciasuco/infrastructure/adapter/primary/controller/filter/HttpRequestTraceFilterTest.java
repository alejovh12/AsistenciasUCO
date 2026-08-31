package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter;

import co.edu.uco.asistenciasuco.infrastructure.observability.tracing.TraceContextSnapshot;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.event.KeyValuePair;
import org.slf4j.MDC;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpRequestTraceFilterTest {

    private final HttpRequestTraceFilter filter = new HttpRequestTraceFilter();

    @Test
    void doFilter_registra_ciclo_y_limpia_trace_mdc() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/grupos");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final AtomicBoolean invoked = new AtomicBoolean(false);
        MDC.put(TraceContextSnapshot.TRACE_ID_MDC_KEY, "trace-stale");
        MDC.put(TraceContextSnapshot.SPAN_ID_MDC_KEY, "span-stale");
        final FilterChain chain = (servletRequest, servletResponse) -> {
            invoked.set(true);
            assertNull(MDC.get(TraceContextSnapshot.TRACE_ID_MDC_KEY));
            assertNull(MDC.get(TraceContextSnapshot.SPAN_ID_MDC_KEY));
            ((MockHttpServletResponse) servletResponse).setStatus(200);
        };

        filter.doFilter(request, response, chain);

        assertTrue(invoked.get());
        assertEquals(200, response.getStatus());
        assertNull(MDC.get(TraceContextSnapshot.TRACE_ID_MDC_KEY));
        assertNull(MDC.get(TraceContextSnapshot.SPAN_ID_MDC_KEY));
    }

    @Test
    void filtro_corre_despues_de_correlationIdFilter() {
        assertEquals(Ordered.HIGHEST_PRECEDENCE + 1, filter.getOrder());
        assertFalse(filter.getOrder() == Ordered.HIGHEST_PRECEDENCE);
    }

    @Test
    void doFilter_clasifica_notFound_como_clientError() throws Exception {
        final ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(HttpRequestTraceFilter.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ruta-que-no-existe");
            final MockHttpServletResponse response = new MockHttpServletResponse();
            final FilterChain chain = (servletRequest, servletResponse) ->
                    ((MockHttpServletResponse) servletResponse).setStatus(404);

            filter.doFilter(request, response, chain);

            assertEquals(404, response.getStatus());
            final ILoggingEvent completedEvent = completedEvent(appender);
            final Map<String, String> keyValues = keyValues(completedEvent);

            assertEquals("HTTP request completed.", completedEvent.getFormattedMessage());
            assertEquals("HTTP_REQUEST_COMPLETED", keyValues.get("eventType"));
            assertEquals("GET", keyValues.get("httpMethod"));
            assertEquals("/ruta-que-no-existe", keyValues.get("path"));
            assertEquals("404", keyValues.get("status"));
            assertEquals("CLIENT_ERROR", keyValues.get("result"));
            assertFalse(keyValues.containsValue("SUCCESS"));
            assertFalse(keyValues.containsValue("SERVER_ERROR"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void doFilter_clasifica_excepcion_inesperada_como_serverError() {
        final ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(HttpRequestTraceFilter.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/fallo");
            final MockHttpServletResponse response = new MockHttpServletResponse();
            final FilterChain chain = (servletRequest, servletResponse) -> {
                throw new ServletException("fallo inesperado");
            };

            assertThrows(ServletException.class, () -> filter.doFilter(request, response, chain));

            final ILoggingEvent completedEvent = completedEvent(appender);
            final Map<String, String> keyValues = keyValues(completedEvent);

            assertEquals("500", keyValues.get("status"));
            assertEquals("SERVER_ERROR", keyValues.get("result"));
            assertNull(MDC.get(TraceContextSnapshot.TRACE_ID_MDC_KEY));
            assertNull(MDC.get(TraceContextSnapshot.SPAN_ID_MDC_KEY));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private ILoggingEvent completedEvent(final ListAppender<ILoggingEvent> appender) {
        return appender.list.stream()
                .filter(event -> "HTTP request completed.".equals(event.getFormattedMessage()))
                .reduce((first, second) -> second)
                .orElseThrow();
    }

    private Map<String, String> keyValues(final ILoggingEvent event) {
        return event.getKeyValuePairs().stream()
                .collect(Collectors.toMap(keyValue -> keyValue.key, keyValue -> String.valueOf(keyValue.value), (left, right) -> right));
    }
}
