package co.edu.uco.notificaciones.integracion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sobre un servidor HTTP real: sondas de salud abiertas, Prometheus y API con clave, cabeceras de correlacion. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.defaults.metrics.export.enabled=true", "management.prometheus.metrics.export.enabled=true"})
class ServidorRealTest extends BaseIntegracion {

    @Value("${local.server.port}")
    private int puerto;

    private HttpResponse<String> get(final String ruta, final String clave, final String correlacion) throws Exception {
        final HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta)).GET();
        if (clave != null) {
            b.header("X-Api-Key", clave);
        }
        if (correlacion != null) {
            b.header("X-Correlation-Id", correlacion);
        }
        return HttpClient.newHttpClient().send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void lasSondasDeSaludSonAbiertas() throws Exception {
        final HttpResponse<String> salud = get("/actuator/health", null, null);
        assertEquals(200, salud.statusCode(), salud.body());
        assertEquals(200, get("/actuator/health/readiness", null, null).statusCode());
        assertEquals(200, get("/actuator/health/liveness", null, null).statusCode());
        assertTrue(salud.body().contains("\"status\":\"UP\""), salud.body());
    }

    @Test
    void prometheusYApiExigenLaClave() throws Exception {
        assertEquals(401, get("/actuator/prometheus", null, null).statusCode());
        final HttpResponse<String> metricas = get("/actuator/prometheus", "clave-integracion", null);
        assertEquals(200, metricas.statusCode());
        assertTrue(metricas.body().contains("jvm_memory_used_bytes"));
        final HttpResponse<String> sinClave = get("/api/v1/estado-canal", null, "corr-real-1");
        assertEquals(401, sinClave.statusCode());
        assertEquals("corr-real-1", sinClave.headers().firstValue("X-Correlation-Id").orElse(""));
        assertTrue(sinClave.body().contains("\"correlationId\":\"corr-real-1\""), sinClave.body());
        final HttpResponse<String> conClave = get("/api/v1/estado-canal", "clave-integracion", null);
        assertEquals(200, conClave.statusCode());
        assertTrue(conClave.body().contains("\"canal\":\"smtp\""), conClave.body());
    }
}
