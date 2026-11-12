package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline;

import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SecurityBaselinePepProperties;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REA-02: el circuit breaker del PEP abre tras fallos repetidos y, abierto, mantiene el fail-closed (decision
 * {@code UNAVAILABLE}, que el filtro traduce a 503) sin volver a llamar al PEP ni esperar su timeout.
 */
class SecurityBaselinePepCircuitBreakerTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void pep_caido_abre_el_breaker_y_las_siguientes_peticiones_fallan_cerrado_sin_llamar_al_pep() throws Exception {
        final AtomicInteger llamadas = new AtomicInteger();
        server = server(503, llamadas);
        final CircuitBreaker breaker = breaker();
        final SecurityBaselinePepClient client = client(breaker);

        assertEquals(PepDecision.Outcome.UNAVAILABLE, client.evaluate(bearerRequest(), "corr-1").outcome());
        assertEquals(PepDecision.Outcome.UNAVAILABLE, client.evaluate(bearerRequest(), "corr-2").outcome());
        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());

        final long inicio = System.nanoTime();
        final PepDecision abierta = client.evaluate(bearerRequest(), "corr-3");
        final Duration duracion = Duration.ofNanos(System.nanoTime() - inicio);

        assertEquals(PepDecision.Outcome.UNAVAILABLE, abierta.outcome(), "abierto nunca es ALLOW: fail-closed");
        assertEquals(2, llamadas.get(), "con el breaker abierto no se consulta el PEP");
        assertTrue(duracion.compareTo(Duration.ofMillis(500)) < 0, "abierto responde sin esperar el timeout del PEP");
        assertEquals(1, breaker.getMetrics().getNumberOfNotPermittedCalls());
    }

    @Test
    void pep_inalcanzable_cuenta_como_fallo_y_abre_el_breaker() {
        final CircuitBreaker breaker = breaker();
        final SecurityBaselinePepClient client = new SecurityBaselinePepClient(HttpClient.newHttpClient(),
                JsonMapper.builder().build(), properties(URI.create("http://127.0.0.1:1")), breaker);

        client.evaluate(bearerRequest(), "corr-1");
        client.evaluate(bearerRequest(), "corr-2");

        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());
        assertEquals(PepDecision.Outcome.UNAVAILABLE, client.evaluate(bearerRequest(), "corr-3").outcome());
    }

    @Test
    void denegaciones_401_y_403_son_respuestas_sanas_y_no_abren_el_breaker() throws Exception {
        final AtomicInteger llamadas = new AtomicInteger();
        server = server(403, llamadas);
        final CircuitBreaker breaker = breaker();
        final SecurityBaselinePepClient client = client(breaker);

        for (int i = 0; i < 4; i++) {
            assertEquals(PepDecision.Outcome.FORBIDDEN, client.evaluate(bearerRequest(), "corr").outcome());
        }
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
        assertEquals(4, llamadas.get());
    }

    @Test
    void sin_bearer_no_consulta_el_pep_ni_toca_el_breaker() {
        final CircuitBreaker breaker = breaker();
        final SecurityBaselinePepClient client = new SecurityBaselinePepClient(HttpClient.newHttpClient(),
                JsonMapper.builder().build(), properties(URI.create("http://127.0.0.1:1")), breaker);

        assertEquals(PepDecision.Outcome.UNAUTHORIZED,
                client.evaluate(new MockHttpServletRequest("GET", "/api/v1/protegido"), "corr").outcome());
        assertEquals(0, breaker.getMetrics().getNumberOfBufferedCalls());
    }

    private SecurityBaselinePepClient client(final CircuitBreaker breaker) {
        return new SecurityBaselinePepClient(HttpClient.newHttpClient(), JsonMapper.builder().build(),
                properties(URI.create("http://127.0.0.1:" + server.getAddress().getPort())), breaker);
    }

    private static CircuitBreaker breaker() {
        return CircuitBreaker.of("securityBaselinePep", CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build());
    }

    private static MockHttpServletRequest bearerRequest() {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/protegido");
        request.addHeader("Authorization", "Bearer token-value");
        return request;
    }

    private static SecurityBaselinePepProperties properties(final URI uri) {
        return new SecurityBaselinePepProperties(uri, "asistencias-uco", "lab", "test-credential",
                Duration.ofSeconds(1), Duration.ofSeconds(2));
    }

    private static HttpServer server(final int status, final AtomicInteger llamadas) throws IOException {
        final HttpServer created = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        created.createContext("/internal/v1/embedded-access-decisions", exchange -> {
            llamadas.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        created.start();
        return created;
    }
}
