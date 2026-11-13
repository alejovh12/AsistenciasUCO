package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline;

import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SecurityBaselinePepProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
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
 * un Bearer, y eso no puede interpretarse como permiso.
 *
 * <p>Solo se acepta el Bearer ya validado por Spring Security como credencial. La cookie de sesión BFF del
 * baseline no se reenvía: el PEP puede resolverla por su canal técnico PEP→PDP, pero el contrato 0.1.7 no entrega
 * al backend una identidad verificable derivada de ella (solo {@code 204 + X-Decision-Id}), así que aceptarla aquí
 * daría una decisión sin identidad autenticada en el backend.</p>
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
    /** REA-02: {@code null} solo en pruebas unitarias del contrato HTTP; el Composition Root siempre lo entrega. */
    private final CircuitBreaker circuitBreaker;

    public SecurityBaselinePepClient(
            final HttpClient httpClient,
            final ObjectMapper objectMapper,
            final SecurityBaselinePepProperties properties
    ) {
        this(httpClient, objectMapper, properties, null);
    }

    public SecurityBaselinePepClient(
            final HttpClient httpClient,
            final ObjectMapper objectMapper,
            final SecurityBaselinePepProperties properties,
            final CircuitBreaker circuitBreaker
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.circuitBreaker = circuitBreaker;
    }

    /**
     * REA-02: con el circuit breaker abierto no se llama al PEP (no se espera {@code request-timeout}) y la decision es
     * {@code UNAVAILABLE}: el filtro sigue respondiendo 503 (fail-closed). Solo cuenta como fallo una decision
     * {@code UNAVAILABLE} (PEP caido, timeout, 5xx o respuesta anomala); 204/401/403 son respuestas sanas del PEP.
     */
    public PepDecision evaluate(final HttpServletRequest request, final String correlationId) {
        final String authorization = singleBearer(request);
        if (authorization == null) {
            // Sin un único Bearer bien formado no hay credencial que evaluar: nunca se degrada a cookie.
            return new PepDecision(PepDecision.Outcome.UNAUTHORIZED, null);
        }
        if (circuitBreaker == null) {
            return consultar(request, authorization, correlationId);
        }
        if (!circuitBreaker.tryAcquirePermission()) {
            return unavailable();
        }
        final long inicio = circuitBreaker.getCurrentTimestamp();
        final PepDecision decision = consultar(request, authorization, correlationId);
        final long duracion = circuitBreaker.getCurrentTimestamp() - inicio;
        if (decision.outcome() == PepDecision.Outcome.UNAVAILABLE) {
            circuitBreaker.onError(duracion, circuitBreaker.getTimestampUnit(), new PepUnavailableException());
        } else {
            circuitBreaker.onSuccess(duracion, circuitBreaker.getTimestampUnit());
        }
        return decision;
    }

    private PepDecision consultar(final HttpServletRequest request, final String authorization, final String correlationId) {
        try {
            final String requestId = UUID.randomUUID().toString();
            final String body = objectMapper.writeValueAsString(new EmbeddedAccessDecisionRawRequest(
                    properties.applicationName(),
                    properties.environment(),
                    request.getRequestURI(),
                    request.getMethod()
            ));
            final HttpRequest pepRequest = HttpRequest.newBuilder()
                    .uri(properties.embeddedDecisionEndpoint())
                    .timeout(properties.requestTimeout())
                    .header("Content-Type", "application/json")
                    .header(AUTHORIZATION, authorization)
                    .header(APPLICATION_CREDENTIAL, properties.applicationCredential())
                    .header(REQUEST_ID, requestId)
                    .header(CORRELATION_ID, isIdentifier(correlationId) ? correlationId : requestId)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            return mapResponse(httpClient.send(pepRequest, HttpResponse.BodyHandlers.discarding()));
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            return unavailable();
        } catch (final IOException | RuntimeException exception) {
            return unavailable();
        }
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

    /** Marca para el circuit breaker; nunca sale de este adaptador ni lleva datos de la peticion. */
    static final class PepUnavailableException extends RuntimeException {
        PepUnavailableException() {
            super("PEP no disponible", null, false, false);
        }
    }

    private record EmbeddedAccessDecisionRawRequest(
            String applicationName,
            String environment,
            String path,
            String method
    ) {
    }
}
