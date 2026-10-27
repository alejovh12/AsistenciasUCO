package co.edu.uco.asistenciasuco.infrastructure.config.security;

import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline.PepDecision;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline.SecurityBaselinePepClient;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

/**
 * Único punto de enforcement MVC del Security Baseline 0.1.7 (PEP embebido). Se registra solo cuando
 * {@code app.adapters.security.provider=baseline}, dentro de la {@code SecurityFilterChain}, después de autenticar
 * el Bearer y antes de {@code AuthorizationFilter}.
 *
 * <p>No existe bandera para apagarlo: en modo baseline toda ruta que no sea pública explícita
 * ({@link SecurityPublicEndpoints#pepExempt()}) exige una decisión {@code ALLOW}. Un PEP/PDP caído responde 503
 * (fail-closed); nunca se permite el acceso por defecto.</p>
 */
public class SecurityPepEnforcementFilter extends OncePerRequestFilter {

    private final SecurityBaselinePepClient baselineClient;
    private final SecurityErrorResponseWriter responseWriter;

    public SecurityPepEnforcementFilter(
            final SecurityBaselinePepClient baselineClient,
            final SecurityErrorResponseWriter responseWriter
    ) {
        this.baselineClient = Objects.requireNonNull(baselineClient, "SecurityBaselinePepClient es obligatorio.");
        this.responseWriter = Objects.requireNonNull(responseWriter, "SecurityErrorResponseWriter es obligatorio.");
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        // El preflight CORS no lleva credenciales y no representa una operación de negocio.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // Baseline protege todo lo que no sea una ruta pública explícita: no depende de prefijos de negocio
        // para evitar que una ruta nueva quede fuera del PEP por olvido.
        return SecurityPublicEndpoints.pepExempt().matches(request);
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isAuthenticated()) {
            // Sin identidad autenticada la cadena de Spring Security responde 401; el PEP no decide por anónimos.
            filterChain.doFilter(request, response);
            return;
        }
        final PepDecision decision = baselineClient.evaluate(request, correlationId(request));
        switch (decision.outcome()) {
            case ALLOW -> {
                response.setHeader("X-Decision-Id", decision.decisionId());
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

    private static boolean isAuthenticated() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private static String correlationId(final HttpServletRequest request) {
        final String current = CorrelationIdContext.getAsString();
        if (current != null && !current.isBlank()) {
            return current;
        }
        final String header = request.getHeader("X-Correlation-Id");
        return header != null && !header.isBlank() ? header : UUID.randomUUID().toString();
    }
}
