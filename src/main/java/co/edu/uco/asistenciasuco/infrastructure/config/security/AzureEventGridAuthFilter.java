package co.edu.uco.asistenciasuco.infrastructure.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Autentica el webhook de Azure Event Grid. La credencial viaja exclusivamente en el header
 * {@value #AEG_SAS_TOKEN_HEADER}; ningún query parameter se considera. Sin credencial
 * configurada (nula, vacía o solo espacios) el filtro rechaza con 401 (fail-closed).
 *
 * <p>Este filtro es la autenticación real del endpoint: el {@code permitAll} de
 * {@link SecurityConfig} solo evita que el JWT de negocio se exija en esta ruta y no equivale
 * a un endpoint sin autenticación.</p>
 */
public class AzureEventGridAuthFilter extends OncePerRequestFilter {

    public static final String WEBHOOK_PATH = "/api/v1/internal/azure-events";
    public static final String AEG_SAS_TOKEN_HEADER = "aeg-sas-token";

    private final byte[] expectedTokenDigest;

    public AzureEventGridAuthFilter(final String expectedToken) {
        final String normalized = expectedToken != null ? expectedToken.trim() : "";
        this.expectedTokenDigest = normalized.isEmpty() ? null : sha256(normalized);
    }

    /**
     * Identifica el webhook de forma inequívoca: la URI, sin el context path, es exactamente
     * {@link #WEBHOOK_PATH}. Una ruta ajena que solo termine igual no coincide.
     */
    public static boolean isWebhookRequest(final HttpServletRequest request) {
        final String requestUri = request.getRequestURI();
        if (requestUri == null) {
            return false;
        }
        final String contextPath = request.getContextPath();
        final String path = (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath))
                ? requestUri.substring(contextPath.length())
                : requestUri;
        return WEBHOOK_PATH.equals(path);
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isWebhookRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (expectedTokenDigest == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Webhook de Azure Event Grid no habilitado o sin token configurado.");
            return;
        }

        final String providedToken = request.getHeader(AEG_SAS_TOKEN_HEADER);
        if (providedToken == null || providedToken.isBlank()
                || !MessageDigest.isEqual(expectedTokenDigest, sha256(providedToken.trim()))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token de autenticacion de Event Grid invalido o faltante.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    // Se comparan digests SHA-256 con MessageDigest.isEqual (tiempo constante, API estándar del JDK):
    // evita filtrar por timing el contenido y la longitud de la credencial esperada.
    private static byte[] sha256(final String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no disponible en la JVM.", exception);
        }
    }
}
