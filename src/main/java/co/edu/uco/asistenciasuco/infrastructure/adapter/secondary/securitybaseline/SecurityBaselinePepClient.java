package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline;

import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SecurityBaselinePepProperties;
import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Adaptador secundario del endpoint PEP embebido de Security Baseline. No conoce PDP, roles ni
 * identidad institucional: solo traduce el contrato HTTP del proveedor a un resultado interno.
 *
 * <p>Contrato v0.1.7 ({@code EmbeddedAccessDecisionController}): una decisión permitida es
 * {@code 204 No Content} con {@code X-Decision-Id}. Cualquier otro 2xx se trata como respuesta
 * anómala (fail-closed): una respuesta {@code 200} vacía, sin decisión, cuando no recibe
 * un Bearer ni una sesión BFF, y eso no puede interpretarse como permiso.
 */
public class SecurityBaselinePepClient {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CORRELATION_ID = "X-Correlation-Id";
    private static final String REQUEST_ID = "X-Request-Id";
    private static final String DECISION_ID = "X-Decision-Id";
    private static final String APPLICATION_CREDENTIAL = "X-Application-Credential";
    /** Mismo formato de identificador que exige el starter oficial para correlación. */
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9_.:-]{1,128}");
    private static final Pattern BEARER_TOKEN = Pattern.compile("[A-Za-z0-9\\-._~+/]+=*");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SecurityBaselinePepProperties properties;

    public SecurityBaselinePepClient(
            final HttpClient httpClient,
            final ObjectMapper objectMapper,
            final SecurityBaselinePepProperties properties
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public PepDecision evaluate(final HttpServletRequest request, final String correlationId) {
        final String authorization = singleBearer(request);
        final String cookie = request.getHeader("Cookie");
        final boolean hasBaselineCookie = cookie != null && cookie.contains("SECURITY_BASELINE_SESSION=");
        final boolean hasAuthorization = request.getHeader(AUTHORIZATION) != null;
        if (authorization == null && (hasAuthorization || !hasBaselineCookie)) {
            // Un Authorization presente pero inválido no puede degradar silenciosamente a cookie.
            // Si no hay Bearer, solo se acepta la cookie HttpOnly que el PEP resuelve por su canal
            // técnico PEP→PDP.
            return new PepDecision(PepDecision.Outcome.UNAUTHORIZED, null);
        }
        try {
            final String requestId = UUID.randomUUID().toString();
            final String body = objectMapper.writeValueAsString(new EmbeddedAccessDecisionRawRequest(
                    properties.applicationName(),
                    properties.environment(),
                    request.getRequestURI(),
                    request.getMethod()
            ));
            HttpRequest pepRequest = HttpRequest.newBuilder()
                    .uri(properties.embeddedDecisionEndpoint())
                    .timeout(properties.requestTimeout())
                    .header("Content-Type", "application/json")
                    .header(APPLICATION_CREDENTIAL, properties.applicationCredential())
                    .header(REQUEST_ID, requestId)
                    .header(CORRELATION_ID, isIdentifier(correlationId) ? correlationId : requestId)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            if (authorization != null) {
                pepRequest = withHeader(pepRequest, AUTHORIZATION, authorization);
            } else {
                pepRequest = withHeader(pepRequest, "Cookie", cookie);
            }
            return mapResponse(httpClient.send(pepRequest, HttpResponse.BodyHandlers.discarding()));
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            return unavailable();
        } catch (final IOException | RuntimeException exception) {
            return unavailable();
        }
    }

    private static HttpRequest withHeader(final HttpRequest request, final String name, final String value) {
        return HttpRequest.newBuilder(request, (key, values) -> true)
                .header(name, value)
                .build();
    }

    private static String singleBearer(final HttpServletRequest request) {
        final Enumeration<String> headers = request.getHeaders(AUTHORIZATION);
        final List<String> values = headers == null ? List.of() : Collections.list(headers);
        if (values.size() != 1) {
            return null;
        }
        final String value = values.getFirst();
        if (value == null || !value.startsWith(BEARER_PREFIX)
                || !BEARER_TOKEN.matcher(value.substring(BEARER_PREFIX.length())).matches()) {
            return null;
        }
        return value;
    }

    private PepDecision mapResponse(final HttpResponse<?> response) {
        final int status = response.statusCode();
        if (status == 204) {
            final String decisionId = response.headers().firstValue(DECISION_ID).orElse(null);
            return isIdentifier(decisionId)
                    ? new PepDecision(PepDecision.Outcome.ALLOW, decisionId)
                    : unavailable();
        }
        if (status == 401) {
            return new PepDecision(PepDecision.Outcome.UNAUTHORIZED, null);
        }
        if (status == 403) {
            return new PepDecision(PepDecision.Outcome.FORBIDDEN, null);
        }
        return unavailable();
    }

    private static boolean isIdentifier(final String value) {
        return value != null && IDENTIFIER.matcher(value).matches();
    }

    private PepDecision unavailable() {
        return new PepDecision(PepDecision.Outcome.UNAVAILABLE, null);
    }

    private record EmbeddedAccessDecisionRawRequest(
            String applicationName,
            String environment,
            String path,
            String method
    ) {
    }
}
