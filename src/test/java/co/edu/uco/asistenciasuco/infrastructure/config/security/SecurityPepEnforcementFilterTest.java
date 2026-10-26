package co.edu.uco.asistenciasuco.infrastructure.config.security;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline.PepDecision;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline.SecurityBaselinePepClient;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Enforcement único del modo baseline: deny-by-default, sin bandera de apagado y fail-closed. */
class SecurityPepEnforcementFilterTest {

    private final SecurityBaselinePepClient client = mock(SecurityBaselinePepClient.class);
    private final SecurityPepEnforcementFilter filter =
            new SecurityPepEnforcementFilter(client, new SecurityErrorResponseWriter());

    @AfterEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        CorrelationIdContext.clear();
    }

    @Test
    void rutas_publicas_y_preflight_no_pasan_por_el_pep() {
        for (final String[] route : new String[][]{
                {"OPTIONS", "/api/v1/grupos"}, {"GET", "/actuator/health"}, {"GET", "/swagger-ui/index.html"},
                {"GET", "/openapi/openapi-golden-path.yaml"}, {"GET", "/error"}}) {
            assertTrue(shouldNotFilter(request(route[0], route[1])), route[0] + " " + route[1]);
        }
    }

    @Test
    void toda_ruta_de_negocio_incluida_una_nueva_exige_decision_pep() {
        for (final String path : new String[]{"/api/v1/grupos", "/api/v2/sesiones/x", "/api/v9/nueva-ruta", "/otra"}) {
            assertFalse(shouldNotFilter(request("GET", path)), path);
        }
    }

    @Test
    void sin_identidad_autenticada_la_cadena_decide_y_el_pep_no_se_consulta() throws Exception {
        final MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request("GET", "/api/v1/grupos"), new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest());
        verify(client, never()).evaluate(any(), anyString());

        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        final MockFilterChain anonymousChain = new MockFilterChain();
        filter.doFilter(request("GET", "/api/v1/grupos"), new MockHttpServletResponse(), anonymousChain);
        assertNotNull(anonymousChain.getRequest());
        verify(client, never()).evaluate(any(), anyString());
    }

    @Test
    void allow_continua_y_expone_el_id_de_decision() throws Exception {
        authenticate();
        when(client.evaluate(any(), anyString())).thenReturn(new PepDecision(PepDecision.Outcome.ALLOW, "dec-1"));
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("POST", "/api/v1/grupos"), response, chain);

        assertNotNull(chain.getRequest());
        assertEquals("dec-1", response.getHeader("X-Decision-Id"));
        assertEquals(200, response.getStatus());
    }

    @Test
    void deny_unauthorized_y_pep_caido_responden_403_401_503_sin_continuar() throws Exception {
        authenticate();
        assertBlocked(PepDecision.Outcome.FORBIDDEN, 403);
        assertBlocked(PepDecision.Outcome.UNAUTHORIZED, 401);
        assertBlocked(PepDecision.Outcome.UNAVAILABLE, 503);
    }

    @Test
    void usa_el_correlation_id_del_contexto() throws Exception {
        authenticate();
        CorrelationIdContext.set("11111111-2222-3333-4444-555555555555");
        when(client.evaluate(any(), anyString())).thenReturn(new PepDecision(PepDecision.Outcome.ALLOW, "d"));

        filter.doFilter(request("GET", "/api/v1/grupos"), new MockHttpServletResponse(), new MockFilterChain());

        verify(client).evaluate(any(), org.mockito.ArgumentMatchers.eq("11111111-2222-3333-4444-555555555555"));
    }

    private void assertBlocked(final PepDecision.Outcome outcome, final int status) throws Exception {
        when(client.evaluate(any(), anyString())).thenReturn(new PepDecision(outcome, null));
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("DELETE", "/api/v1/grupos/1"), response, chain);

        assertNull(chain.getRequest(), outcome.name());
        assertEquals(status, response.getStatus(), outcome.name());
    }

    private boolean shouldNotFilter(final MockHttpServletRequest request) {
        return new ExposedFilter().exposedShouldNotFilter(request);
    }

    private static void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "n/a", "ROLE_DOCENTE"));
    }

    private static MockHttpServletRequest request(final String method, final String path) {
        final MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        request.setServletPath(path);
        return request;
    }

    /** Acceso al método protegido sin cambiar la visibilidad en producción. */
    private final class ExposedFilter extends SecurityPepEnforcementFilter {
        ExposedFilter() {
            super(client, new SecurityErrorResponseWriter());
        }

        boolean exposedShouldNotFilter(final MockHttpServletRequest request) {
            return shouldNotFilter(request);
        }
    }
}
