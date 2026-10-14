package co.edu.uco.asistenciasuco.infrastructure.config.security;

import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline.PepDecision;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline.SecurityBaselinePepClient;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SecurityBaselinePepProperties;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Enforcement servlet opt-in. El filtro agrega una decisión PEP; no autentica ni concede roles.
 *
 * <p>Se ejecuta solo dentro de la {@code SecurityFilterChain}, inmediatamente después del
 * {@code BearerTokenAuthenticationFilter}: su registro automático como filtro servlet está
 * deshabilitado en {@code SecurityBaselinePepConfiguration}. Toda ruta no declarada pública en
 * {@link SecurityPublicEndpoints} exige una decisión permitida; los dispatches ASYNC/ERROR
 * heredan la decisión de su REQUEST original (comportamiento de {@link OncePerRequestFilter}).
 */
public final class SecurityBaselinePepFilter extends OncePerRequestFilter {

    private static final String DECISION_ID = "X-Decision-Id";

    private final SecurityBaselinePepClient client;
    private final SecurityBaselinePepProperties properties;
    private final SecurityErrorResponseWriter responseWriter;
    private final RequestMatcher exempt;

    public SecurityBaselinePepFilter(
            final SecurityBaselinePepClient client,
            final SecurityBaselinePepProperties properties,
            final SecurityErrorResponseWriter responseWriter
    ) {
        this.client = client;
        this.properties = properties;
        this.responseWriter = responseWriter;
        this.exempt = SecurityPublicEndpoints.pepExempt();
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        return !properties.enabled()
                || CorsUtils.isPreFlightRequest(request)
                || exempt.matches(request);
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isAuthenticated()) {
            // Sin autenticación válida decide el resource server / AuthorizationFilter (401).
            filterChain.doFilter(request, response);
            return;
        }

        final PepDecision decision = client.evaluate(request, correlationId());
        switch (decision.outcome()) {
            case ALLOW -> {
                response.setHeader(DECISION_ID, decision.decisionId());
                filterChain.doFilter(request, response);
            }
            case UNAUTHORIZED -> responseWriter.write(
                    request, response, HttpStatus.UNAUTHORIZED, SecurityErrorCode.UNAUTHORIZED);
            case FORBIDDEN -> responseWriter.write(
                    request, response, HttpStatus.FORBIDDEN, SecurityErrorCode.FORBIDDEN);
            case UNAVAILABLE -> responseWriter.write(
                    request, response, HttpStatus.SERVICE_UNAVAILABLE, SecurityErrorCode.ACCESS_DECISION_UNAVAILABLE);
        }
    }

    private boolean isAuthenticated() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    /** Solo el valor ya saneado por {@code CorrelationIdFilter}; nunca el header crudo. */
    private String correlationId() {
        final String contextValue = CorrelationIdContext.getAsString();
        return contextValue == null || contextValue.isBlank() ? UUID.randomUUID().toString() : contextValue;
    }
}
