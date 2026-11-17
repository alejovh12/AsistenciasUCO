package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline;

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
    void cookie_de_sesion_baseline_sin_bearer_no_se_consulta_ni_se_reenvia() throws Exception {
        final AtomicReference<Boolean> called = new AtomicReference<>(false);
        server = server(exchange -> {
            called.set(true);
            exchange.getResponseHeaders().add("X-Decision-Id", "decision-cookie");
            exchange.sendResponseHeaders(204, -1);
        });

        final SecurityBaselinePepClient client = new SecurityBaselinePepClient(
                HttpClient.newHttpClient(), JsonMapper.builder().build(), properties());
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/protegido");
        request.addHeader("Cookie", "SECURITY_BASELINE_SESSION=session-value; otro=1");

        final PepDecision decision = client.evaluate(request, "corr-cookie");

        assertEquals(PepDecision.Outcome.UNAUTHORIZED, decision.outcome());
        assertFalse(called.get(), "sin Bearer no debe haber consulta al PEP");
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
                {"200", "decision-1"},   // 2xx sin decisión: fail-closed
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
            } catch (final InterruptedException _) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(204, -1);
        });
        final SecurityBaselinePepClient client = new SecurityBaselinePepClient(
                HttpClient.newHttpClient(), JsonMapper.builder().build(), new SecurityBaselinePepProperties(
                serverUri(), "asistencias-uco", "lab", "test-credential",
                Duration.ofSeconds(1), Duration.ofMillis(300)));

        assertEquals(PepDecision.Outcome.UNAVAILABLE, client.evaluate(bearerRequest(), "corr-123").outcome());
    }

    /**
     * VAL-oleada7 (H4): con el PEP deteniéndose, una petición en vuelo esperó más de 30 s pese a {@code request-timeout}.
     * Se reproducen las dos formas de quedarse colgado con un socket crudo y el mismo cliente HTTP/1.1 de producción:
     * conexión aceptada sin respuesta, y encabezados enviados con el cuerpo a medias. En ambos casos la decisión debe ser
     * UNAVAILABLE dentro de {@code request-timeout} más un margen, nunca esperar a que el PEP cierre la conexión.
     */
    @Test
    void pep_que_acepta_la_conexion_y_no_responde_se_corta_en_request_timeout() throws Exception {
        assertCorteEnRequestTimeout(socket -> { });
    }

    @Test
    void pep_que_envia_encabezados_y_deja_el_cuerpo_a_medias_se_corta_en_request_timeout() throws Exception {
        assertCorteEnRequestTimeout(socket -> {
            final java.io.OutputStream out = socket.getOutputStream();
            out.write(("HTTP/1.1 204 No Content\r\nX-Decision-Id: decision-1\r\n").getBytes(
                    java.nio.charset.StandardCharsets.US_ASCII));
            out.flush();
        });
        assertCorteEnRequestTimeout(socket -> {
            final java.io.OutputStream out = socket.getOutputStream();
            out.write(("HTTP/1.1 200 OK\r\nX-Decision-Id: decision-1\r\nContent-Length: 64\r\n\r\nparcial").getBytes(
                    java.nio.charset.StandardCharsets.US_ASCII));
            out.flush();
        });
    }

    private void assertCorteEnRequestTimeout(final SocketColgado comportamiento) throws Exception {
        final Duration requestTimeout = Duration.ofMillis(500);
        try (java.net.ServerSocket colgado = new java.net.ServerSocket(0, 50,
                java.net.InetAddress.getLoopbackAddress())) {
            final java.util.concurrent.CopyOnWriteArrayList<java.net.Socket> abiertos =
                    new java.util.concurrent.CopyOnWriteArrayList<>();
            final Thread aceptador = Thread.ofVirtual().start(() -> {
                try {
                    while (!colgado.isClosed()) {
                        final java.net.Socket socket = colgado.accept();
                        abiertos.add(socket);
                        comportamiento.aplicar(socket);
                    }
                } catch (final IOException _) {
                    // El ServerSocket se cerró al terminar la prueba.
                }
            });
            try {
                final SecurityBaselinePepClient client = new SecurityBaselinePepClient(
                        HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(1))
                                .version(HttpClient.Version.HTTP_1_1)
                                .followRedirects(HttpClient.Redirect.NEVER)
                                .build(),
                        JsonMapper.builder().build(),
                        new SecurityBaselinePepProperties(
                                java.net.URI.create("http://127.0.0.1:" + colgado.getLocalPort()),
                                "asistencias-uco", "lab", "test-credential", Duration.ofSeconds(1), requestTimeout));

                final long inicio = System.nanoTime();
                final PepDecision decision = org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(
                        Duration.ofSeconds(5), () -> client.evaluate(bearerRequest(), "corr-h4"),
                        "el cliente del PEP siguió esperando después de request-timeout");
                final Duration espera = Duration.ofNanos(System.nanoTime() - inicio);

                assertEquals(PepDecision.Outcome.UNAVAILABLE, decision.outcome());
                assertTrue(espera.compareTo(requestTimeout.plusMillis(1_500)) < 0,
                        "esperó " + espera.toMillis() + " ms con request-timeout de " + requestTimeout.toMillis() + " ms");
            } finally {
                colgado.close();
                for (final java.net.Socket socket : abiertos) {
                    socket.close();
                }
                aceptador.join(Duration.ofSeconds(2));
            }
        }
    }

    @FunctionalInterface
    private interface SocketColgado {
        void aplicar(java.net.Socket socket) throws IOException;
    }

    private MockHttpServletRequest bearerRequest() {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/protegido");
        request.addHeader("Authorization", "Bearer token-value");
        return request;
    }

    private SecurityBaselinePepProperties properties() {
        return new SecurityBaselinePepProperties(
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
