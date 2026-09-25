package co.edu.uco.asistenciasuco.infrastructure.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AzureEventGridAuthFilter extends OncePerRequestFilter {

    public static final String WEBHOOK_PATH = "/api/v1/internal/azure-events";
    public static final String AEG_SAS_TOKEN_HEADER = "aeg-sas-token";

    private final String expectedToken;

    public AzureEventGridAuthFilter(final String expectedToken) {
        this.expectedToken = expectedToken != null ? expectedToken.trim() : "";
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        final String requestUri = request.getRequestURI();
        if (requestUri == null || !requestUri.endsWith(WEBHOOK_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (expectedToken.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Webhook de Azure Event Grid no habilitado o sin token configurado.");
            return;
        }

        final String tokenHeader = request.getHeader(AEG_SAS_TOKEN_HEADER);
        final String tokenParam = request.getParameter("token");
        final String providedToken = (tokenHeader != null && !tokenHeader.isBlank()) ? tokenHeader.trim() : tokenParam;

        if (providedToken == null || !expectedToken.equals(providedToken.trim())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token de autenticacion de Event Grid invalido o faltante.");
            return;
        }

        filterChain.doFilter(request, response);
    }
}