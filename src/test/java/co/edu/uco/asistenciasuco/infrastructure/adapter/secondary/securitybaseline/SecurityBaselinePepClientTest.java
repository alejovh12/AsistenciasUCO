package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SecurityBaselinePepProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityBaselinePepClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void envia_contrato_raw_y_no_reenvia_cookie() throws Exception {
        final AtomicReference<String> method = new AtomicReference<>();
        final AtomicReference<String> path = new AtomicReference<>();
        final AtomicReference<String> body = new AtomicReference<>();
        final AtomicReference<String> authorization = new AtomicReference<>();
        final AtomicReference<String> credential = new AtomicReference<>();
        final AtomicReference<String> requestId = new AtomicReference<>();
        final AtomicReference<String> cookie = new AtomicReference<>();
        server = server(exchange -> {
            method.set(exchange.getRequestMethod());
            path.set(exchange.getRequestURI().getPath());
            body.set(new String(exchange.getRequestBody().readAllBytes()));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            credential.set(exchange.getRequestHeaders().getFirst("X-Application-Credential"));
            requestId.set(exchange.getRequestHeaders().getFirst("X-Request-Id"));
            lastCorrelationHeader.set(exchange.getRequestHeaders().getFirst("X-Correlation-Id"));
            cookie.set(exchange.getRequestHeaders().getFirst("Cookie"));
            exchange.getResponseHeaders().add("X-Decision-Id", "decision-123");
            exchange.sendResponseHeaders(204, -1);
        });

        final SecurityBaselinePepProperties properties = properties();
        final SecurityBaselinePepClient client = new SecurityBaselinePepClient(
                HttpClient.newHttpClient(), JsonMapper.builder().build(), properties);
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/grupos");
        request.addHeader("Authorization", "Bearer token-value");
        request.addHeader("Cookie", "SESSION=must-not-forward");

        final PepDecision decision = client.evaluate(request, "corr-123");

        assertEquals(PepDecision.Outcome.ALLOW, decision.outcome());
        assertEquals("decision-123", decision.decisionId());
        assertEquals("POST", method.get());
        assertEquals("/internal/v1/embedded-access-decisions", path.get());
        assertEquals("Bearer token-value", authorization.get());
        assertEquals("test-credential", credential.get());
        assertTrue(requestId.get() != null && !requestId.get().isBlank());
        assertEquals("corr-123", serverCorrelationHeader());
        assertEquals(null, cookie.get());
        assertTrue(body.get().contains("\"applicationName\":\"asistencias-uco\""));
        assertTrue(body.get().contains("\"environment\":\"lab\""));
        assertTrue(body.get().contains("\"path\":\"/api/v1/grupos\""));
        assertTrue(body.get().contains("\"method\":\"POST\""));
        assertFalse(body.get().contains("token-value"));
        assertFalse(body.get().contains("test-credential"));
    }

    @Test
    void mapea_denegacion_y_fallo_del_pep_sin_bypass() throws Exception {
        for (final int status : new int[]{401, 403, 500}) {
            server = server(exchange -> {
                exchange.sendResponseHeaders(status, -1);
            });
            final SecurityBaselinePepClient client = new SecurityBaselinePepClient(
                    HttpClient.newHttpClient(), JsonMapper.builder().build(), properties());
            final PepDecision decision = client.evaluate(bearerRequest(), "corr-123");

            final PepDecision.Outcome expected = status == 401
                    ? PepDecision.Outcome.UNAUTHORIZED
                    : status == 403 ? PepDecision.Outcome.FORBIDDEN : PepDecision.Outcome.UNAVAILABLE;
            assertEquals(expected, decision.outcome());
            server.stop(0);
            server = null;
        }
    }

    @Test
    void solo_204_con_decision_id_valido_es_allow() throws Exception {
        final String[][] cases = {
                {"200", "decision-1"},   // PEP v0.1.6 sin Bearer responde 2xx sin decisión
                {"202", "decision-1"},
                {"204", null},
                {"204", "id con espacios"},
                {"307", "decision-1"}
        };
        for (final String[] anomaly : cases) {
            server = server(exchange -> {
                if (anomaly[1] != null) {
                    exchange.getResponseHeaders().add("X-Decision-Id", anomaly[1]);
                }
                if ("307".equals(anomaly[0])) {
                    exchange.getResponseHeaders().add("Location", "http://127.0.0.1:1/elsewhere");
                }
                exchange.sendResponseHeaders(Integer.parseInt(anomaly[0]), -1);
            });
            final SecurityBaselinePepClient client = new SecurityBaselinePepClient(
                    HttpClient.newHttpClient(), JsonMapper.builder().build(), properties());

            final PepDecision decision = client.evaluate(bearerRequest(), "corr-123");

            assertEquals(PepDecision.Outcome.UNAVAILABLE, decision.outcome(), "status " + anomaly[0]);
            assertNull(decision.decisionId());
            server.stop(0);
            server = null;
        }
    }

    @Test
    void sin_un_unico_bearer_bien_formado_no_consulta_al_pep() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        server = server(exchange -> {
            calls.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
        });
        final SecurityBaselinePepClient client = new SecurityBaselinePepClient(
                HttpClient.newHttpClient(), JsonMapper.builder().build(), properties());
        final MockHttpServletRequest none = new MockHttpServletRequest("GET", "/api/v1/protegido");
        final MockHttpServletRequest basic = new MockHttpServletRequest("GET", "/api/v1/protegido");
        basic.addHeader("Authorization", "Basic Zm9vOmJhcg==");
        final MockHttpServletRequest twice = bearerRequest();
        twice.addHeader("Authorization", "Bearer other");
        final MockHttpServletRequest injected = new MockHttpServletRequest("GET", "/api/v1/protegido");
        injected.addHeader("Authorization", "Bearer abc def");

        for (final MockHttpServletRequest request : List.of(none, basic, twice, injected)) {
            assertEquals(PepDecision.Outcome.UNAUTHORIZED, client.evaluate(request, "corr-123").outcome());
        }
        assertEquals(0, calls.get());
    }

    @Test
    void correlation_invalido_se_sustituye_por_request_id() throws Exception {
        final AtomicReference<String> correlation = new AtomicReference<>();
        final AtomicReference<String> requestId = new AtomicReference<>();
        server = server(exchange -> {
            correlation.set(exchange.getRequestHeaders().getFirst("X-Correlation-Id"));
            requestId.set(exchange.getRequestHeaders().getFirst("X-Request-Id"));
            exchange.getResponseHeaders().add("X-Decision-Id", "decision-1");
            exchange.sendResponseHeaders(204, -1);
        });
        final SecurityBaselinePepClient client = new SecurityBaselinePepClient(
                HttpClient.newHttpClient(), JsonMapper.builder().build(), properties());

        client.evaluate(bearerRequest(), "valor con espacios");

        assertEquals(requestId.get(), correlation.get());
    }

    @Test
    void pep_que_no_responde_a_tiempo_es_unavailable() throws Exception {
        server = server(exchange -> {
            try {
                Thread.sleep(1_500);
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(204, -1);
        });
        final SecurityBaselinePepClient client = new SecurityBaselinePepClient(
                HttpClient.newHttpClient(), JsonMapper.builder().build(), new SecurityBaselinePepProperties(
                true, serverUri(), "asistencias-uco", "lab", "test-credential",
                Duration.ofSeconds(1), Duration.ofMillis(300)));

        assertEquals(PepDecision.Outcome.UNAVAILABLE, client.evaluate(bearerRequest(), "corr-123").outcome());
    }

    private MockHttpServletRequest bearerRequest() {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/protegido");
        request.addHeader("Authorization", "Bearer token-value");
        return request;
    }

    private SecurityBaselinePepProperties properties() {
        return new SecurityBaselinePepProperties(
                true,
                serverUri(),
                "asistencias-uco",
                "lab",
                "test-credential",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2));
    }

    private java.net.URI serverUri() {
        return java.net.URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    private HttpServer server(final com.sun.net.httpserver.HttpHandler handler) throws IOException {
        final HttpServer created = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        created.createContext("/internal/v1/embedded-access-decisions", handler);
        created.start();
        return created;
    }

    private String serverCorrelationHeader() {
        return lastCorrelationHeader.get();
    }

    private final AtomicReference<String> lastCorrelationHeader = new AtomicReference<>();
}
