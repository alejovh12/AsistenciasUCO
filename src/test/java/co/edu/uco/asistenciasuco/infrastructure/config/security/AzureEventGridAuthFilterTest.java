package co.edu.uco.asistenciasuco.infrastructure.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AzureEventGridAuthFilterTest {

    private static final String VALID_TOKEN = "super-secret-token";
    private AzureEventGridAuthFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new AzureEventGridAuthFilter(VALID_TOKEN);
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        filterChain = Mockito.mock(FilterChain.class);
    }

    @Test
    void permite_otros_endpoints_sin_validar_token() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/sesiones");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), Mockito.anyString());
    }

    @Test
    void rechaza_con_401_si_falta_token_en_webhook() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/internal/azure-events");
        when(request.getHeader("aeg-sas-token")).thenReturn(null);
        when(request.getParameter("token")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), contains("Token de autenticacion"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void permite_acceso_cuando_header_aeg_sas_token_es_valido() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/internal/azure-events");
        when(request.getHeader("aeg-sas-token")).thenReturn(VALID_TOKEN);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), Mockito.anyString());
    }
}