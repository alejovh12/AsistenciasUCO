package co.edu.uco.asistenciasuco.infrastructure.config.health;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** OBS-10 (VAL-oleada8): indicador de salud del emisor de identidad, contra un servidor HTTP local. */
class EmisorIdentidadHealthIndicatorTest {

    private HttpServer servidor;
    private final AtomicReference<String> rutaPedida = new AtomicReference<>();

    @AfterEach
    void detener() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private String emisorQueResponde(final int estado) throws Exception {
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        servidor.createContext("/", intercambio -> {
            rutaPedida.set(intercambio.getRequestURI().getPath());
            final byte[] cuerpo = "{\"issuer\":\"x\"}".getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(estado, cuerpo.length);
            intercambio.getResponseBody().write(cuerpo);
            intercambio.close();
        });
        servidor.start();
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/realms/security-baseline/";
    }

    @Test
    void documento_de_descubrimiento_200_da_up_sin_la_barra_final_duplicada() throws Exception {
        final String emisor = emisorQueResponde(200);

        final Health salud = new EmisorIdentidadHealthIndicator(emisor).health();

        assertEquals(Status.UP, salud.getStatus());
        assertEquals("/realms/security-baseline/.well-known/openid-configuration", rutaPedida.get());
        assertEquals(emisor.substring(0, emisor.length() - 1), salud.getDetails().get("issuer"));
        assertEquals(200, salud.getDetails().get("status"));
    }

    @Test
    void respuesta_distinta_de_200_da_down_con_el_estado() throws Exception {
        final Health salud = new EmisorIdentidadHealthIndicator(emisorQueResponde(503)).health();

        assertEquals(Status.DOWN, salud.getStatus());
        assertEquals(503, salud.getDetails().get("status"));
    }

    @Test
    void emisor_inalcanzable_da_down_con_el_tipo_de_error() throws Exception {
        final int puertoLibre;
        try (ServerSocket temporal = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            puertoLibre = temporal.getLocalPort();
        }

        final Health salud = new EmisorIdentidadHealthIndicator("http://127.0.0.1:" + puertoLibre + "/realms/x").health();

        assertEquals(Status.DOWN, salud.getStatus());
        assertEquals("ConnectException", salud.getDetails().get("error"));
    }

    @Test
    void sin_emisor_no_se_construye() {
        assertThrows(NullPointerException.class, () -> new EmisorIdentidadHealthIndicator(null));
    }
}
