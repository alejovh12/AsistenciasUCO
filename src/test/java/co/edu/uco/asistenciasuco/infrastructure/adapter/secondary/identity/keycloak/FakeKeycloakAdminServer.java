package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.identity.keycloak;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servidor HTTP real (JDK {@code com.sun.net.httpserver}, sin dependencias nuevas) que simula
 * la Admin REST API de Keycloak para probar {@link KeycloakIdentityProviderAdapter} de punta a
 * punta sin conectarse a un Keycloak real. El adapter construye su propio
 * {@code java.net.http.HttpClient} internamente (no es inyectable), así que un mock no sirve
 * aquí — se necesita un servidor real, aunque sea uno falso.
 *
 * <p>Se registran respuestas exactas por {@code "METODO path?query"} y se registran todas las
 * requests recibidas para poder verificar qué llamó el adapter (y con qué cuerpo/headers).</p>
 */
final class FakeKeycloakAdminServer implements AutoCloseable {

    private final HttpServer server;
    private final Map<String, StubbedResponse> stubs = new LinkedHashMap<>();
    private final Map<String, StubbedResponse> firstStubs = new LinkedHashMap<>();
    private final List<RecordedRequest> recordedRequests = new CopyOnWriteArrayList<>();

    FakeKeycloakAdminServer() {
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
        server.createContext("/", this::dispatch);
        server.setExecutor(null);
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    void stubJson(final String method, final String pathWithQuery, final int status, final String jsonBody) {
        stub(method, pathWithQuery, status, jsonBody, Map.of("Content-Type", "application/json"));
    }

    void stubFirstJson(final String method, final String pathWithQuery, final int status, final String jsonBody) {
        firstStubs.put(key(method, pathWithQuery),
                new StubbedResponse(status, jsonBody, Map.of("Content-Type", "application/json")));
    }

    void stubNoContent(final String method, final String pathWithQuery, final int status) {
        stub(method, pathWithQuery, status, "", Map.of());
    }

    void stubCreated(final String method, final String pathWithQuery, final String locationHeader) {
        stub(method, pathWithQuery, 201, "", Map.of("Location", locationHeader));
    }

    void stub(
            final String method,
            final String pathWithQuery,
            final int status,
            final String body,
            final Map<String, String> headers
    ) {
        stubs.put(key(method, pathWithQuery), new StubbedResponse(status, body, headers));
    }

    boolean wasCalled(final String method, final String pathWithQuery) {
        return recordedRequests.stream()
                .anyMatch(request -> request.method().equals(method) && request.pathWithQuery().equals(pathWithQuery));
    }

    int callCount(final String method, final String pathWithQuery) {
        return (int) recordedRequests.stream()
                .filter(request -> request.method().equals(method) && request.pathWithQuery().equals(pathWithQuery))
                .count();
    }

    String lastRequestBody(final String method, final String pathWithQuery) {
        return recordedRequests.stream()
                .filter(request -> request.method().equals(method) && request.pathWithQuery().equals(pathWithQuery))
                .reduce((first, second) -> second)
                .map(RecordedRequest::body)
                .orElse(null);
    }

    String lastAuthorizationHeader(final String method, final String pathWithQuery) {
        return recordedRequests.stream()
                .filter(request -> request.method().equals(method) && request.pathWithQuery().equals(pathWithQuery))
                .reduce((first, second) -> second)
                .map(RecordedRequest::authorizationHeader)
                .orElse(null);
    }

    private void dispatch(final HttpExchange exchange) throws IOException {
        final String method = exchange.getRequestMethod();
        final String rawQuery = exchange.getRequestURI().getRawQuery();
        final String pathWithQuery = exchange.getRequestURI().getRawPath() + (rawQuery == null ? "" : "?" + rawQuery);
        final String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        final String authorizationHeader = exchange.getRequestHeaders().getFirst("Authorization");
        recordedRequests.add(new RecordedRequest(method, pathWithQuery, body, authorizationHeader));

        final String requestKey = key(method, pathWithQuery);
        final StubbedResponse stubbed = callCount(method, pathWithQuery) == 1
                && firstStubs.containsKey(requestKey)
                ? firstStubs.get(requestKey) : stubs.get(requestKey);
        if (stubbed == null) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        stubbed.headers().forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
        final byte[] payload = stubbed.body() == null ? new byte[0] : stubbed.body().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(stubbed.status(), payload.length == 0 ? -1 : payload.length);
        if (payload.length > 0) {
            exchange.getResponseBody().write(payload);
        }
        exchange.close();
    }

    private static String key(final String method, final String pathWithQuery) {
        return method + " " + pathWithQuery;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private record StubbedResponse(int status, String body, Map<String, String> headers) {
    }

    private record RecordedRequest(String method, String pathWithQuery, String body, String authorizationHeader) {
    }
}
