package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.correlation.CorrelationIdContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();
    private static final String VALID_CORRELATION_ID = "93641bab-e3cd-485c-b275-47e7b731e18c";
    private static final String ZERO_CORRELATION_ID = "00000000-0000-0000-0000-000000000000";

    @Test
    void doFilter_usa_header_uuid_valido_y_limpia_mdc_al_finalizar() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final AtomicReference<String> correlationIdDuranteCadena = new AtomicReference<>();
        final AtomicReference<String> contextDuranteCadena = new AtomicReference<>();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, VALID_CORRELATION_ID);
        final FilterChain chain = (servletRequest, servletResponse) -> {
            correlationIdDuranteCadena.set(MDC.get(CorrelationIdFilter.MDC_KEY));
            contextDuranteCadena.set(CorrelationIdContext.require().toString());
        };

        filter.doFilter(request, response, chain);

        assertEquals(VALID_CORRELATION_ID, response.getHeader(CorrelationIdFilter.HEADER_NAME));
        assertEquals(VALID_CORRELATION_ID, correlationIdDuranteCadena.get());
        assertEquals(VALID_CORRELATION_ID, contextDuranteCadena.get());
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
        assertNull(CorrelationIdContext.get());
    }

    @Test
    void doFilter_genera_correlationId_cuando_no_llega_header() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final AtomicReference<String> correlationIdDuranteCadena = new AtomicReference<>();
        final FilterChain chain = (servletRequest, servletResponse) ->
                correlationIdDuranteCadena.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertNotNull(response.getHeader(CorrelationIdFilter.HEADER_NAME));
        assertEquals(response.getHeader(CorrelationIdFilter.HEADER_NAME), correlationIdDuranteCadena.get());
        assertNotNull(java.util.UUID.fromString(response.getHeader(CorrelationIdFilter.HEADER_NAME)));
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
        assertNull(CorrelationIdContext.get());
    }

    @Test
    void doFilter_reutiliza_exactamente_header_uuid_valido_en_mayusculas() throws Exception {
        final String headerValido = "93641BAB-E3CD-485C-B275-47E7B731E18C";
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final AtomicReference<String> correlationIdDuranteCadena = new AtomicReference<>();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, headerValido);
        final FilterChain chain = (servletRequest, servletResponse) ->
                correlationIdDuranteCadena.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertEquals(headerValido, response.getHeader(CorrelationIdFilter.HEADER_NAME));
        assertEquals(headerValido, correlationIdDuranteCadena.get());
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
        assertNull(CorrelationIdContext.get());
    }

    @Test
    void doFilter_genera_correlationId_cuando_header_es_invalido() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final AtomicReference<String> correlationIdDuranteCadena = new AtomicReference<>();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "corr-456");
        final FilterChain chain = (servletRequest, servletResponse) ->
                correlationIdDuranteCadena.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertNotNull(response.getHeader(CorrelationIdFilter.HEADER_NAME));
        assertNotEquals("corr-456", response.getHeader(CorrelationIdFilter.HEADER_NAME));
        assertEquals(response.getHeader(CorrelationIdFilter.HEADER_NAME), correlationIdDuranteCadena.get());
        assertNotNull(java.util.UUID.fromString(response.getHeader(CorrelationIdFilter.HEADER_NAME)));
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
        assertNull(CorrelationIdContext.get());
    }

    @Test
    void doFilter_genera_correlationId_cuando_header_tiene_espacios() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final AtomicReference<String> correlationIdDuranteCadena = new AtomicReference<>();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, " " + VALID_CORRELATION_ID + " ");
        final FilterChain chain = (servletRequest, servletResponse) ->
                correlationIdDuranteCadena.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertNotEquals(VALID_CORRELATION_ID, response.getHeader(CorrelationIdFilter.HEADER_NAME));
        assertEquals(response.getHeader(CorrelationIdFilter.HEADER_NAME), correlationIdDuranteCadena.get());
    }

    @Test
    void doFilter_genera_correlationId_cuando_header_contiene_crlf() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final AtomicReference<String> correlationIdDuranteCadena = new AtomicReference<>();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, VALID_CORRELATION_ID + "\r\nX-Evil: injected");
        final FilterChain chain = (servletRequest, servletResponse) ->
                correlationIdDuranteCadena.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertNotEquals(VALID_CORRELATION_ID + "\r\nX-Evil: injected", response.getHeader(CorrelationIdFilter.HEADER_NAME));
        assertEquals(response.getHeader(CorrelationIdFilter.HEADER_NAME), correlationIdDuranteCadena.get());
        assertNotNull(java.util.UUID.fromString(response.getHeader(CorrelationIdFilter.HEADER_NAME)));
    }

    @Test
    void doFilter_genera_correlationId_cuando_header_es_uuid_cero() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final AtomicReference<String> correlationIdDuranteCadena = new AtomicReference<>();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, ZERO_CORRELATION_ID);
        final FilterChain chain = (servletRequest, servletResponse) ->
                correlationIdDuranteCadena.set(CorrelationIdContext.require().toString());

        filter.doFilter(request, response, chain);

        assertNotNull(response.getHeader(CorrelationIdFilter.HEADER_NAME));
        assertNotEquals(ZERO_CORRELATION_ID, response.getHeader(CorrelationIdFilter.HEADER_NAME));
        assertEquals(response.getHeader(CorrelationIdFilter.HEADER_NAME), correlationIdDuranteCadena.get());
        assertNotNull(java.util.UUID.fromString(response.getHeader(CorrelationIdFilter.HEADER_NAME)));
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
        assertNull(CorrelationIdContext.get());
    }

    @Test
    void doFilter_limpia_mdc_y_contexto_si_la_cadena_lanza_excepcion() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, VALID_CORRELATION_ID);
        final FilterChain chain = (servletRequest, servletResponse) -> {
            assertEquals(VALID_CORRELATION_ID, MDC.get(CorrelationIdFilter.MDC_KEY));
            assertEquals(VALID_CORRELATION_ID, CorrelationIdContext.getAsString());
            throw new ServletException("fallo controlado");
        };

        assertThrows(ServletException.class, () -> filter.doFilter(request, response, chain));

        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
        assertNull(CorrelationIdContext.get());
        assertEquals(VALID_CORRELATION_ID, response.getHeader(CorrelationIdFilter.HEADER_NAME));
    }

    @Test
    void doFilter_requests_consecutivos_no_comparten_correlationId() throws Exception {
        final AtomicReference<String> correlationA = new AtomicReference<>();
        final AtomicReference<String> correlationB = new AtomicReference<>();

        final MockHttpServletRequest requestA = new MockHttpServletRequest();
        requestA.addHeader(CorrelationIdFilter.HEADER_NAME, VALID_CORRELATION_ID);
        filter.doFilter(requestA, new MockHttpServletResponse(), (servletRequest, servletResponse) ->
                correlationA.set(MDC.get(CorrelationIdFilter.MDC_KEY)));

        final MockHttpServletRequest requestB = new MockHttpServletRequest();
        filter.doFilter(requestB, new MockHttpServletResponse(), (servletRequest, servletResponse) ->
                correlationB.set(MDC.get(CorrelationIdFilter.MDC_KEY)));

        assertEquals(VALID_CORRELATION_ID, correlationA.get());
        assertNotNull(correlationB.get());
        assertNotEquals(correlationA.get(), correlationB.get());
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
        assertNull(CorrelationIdContext.get());
    }

    @Test
    void context_require_no_genera_uuid_fuera_de_request_http() {
        CorrelationIdContext.clear();

        assertThrows(CrosscuttingException.class, CorrelationIdContext::require);
        assertNull(CorrelationIdContext.get());
        assertNull(MDC.get(CorrelationIdContext.MDC_KEY));
    }

    @Test
    void filtro_tiene_maxima_prioridad() {
        assertEquals(Ordered.HIGHEST_PRECEDENCE, filter.getOrder());
    }
}
