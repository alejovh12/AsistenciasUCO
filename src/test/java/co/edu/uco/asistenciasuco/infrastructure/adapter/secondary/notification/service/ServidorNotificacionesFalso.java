package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/** Servicio de Notificaciones simulado sobre el servidor HTTP del JDK: registra cada peticion y responde lo programado. */
final class ServidorNotificacionesFalso implements AutoCloseable {

    record Peticion(String metodo, String ruta, String consulta, Map<String, List<String>> cabeceras, String cuerpo) {
        String cabecera(final String nombre) {
            return cabeceras.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(nombre))
                    .map(e -> e.getValue().getFirst())
                    .findFirst().orElse(null);
        }
    }

    record Respuesta(int status, String cuerpo, long demoraMs) {
        static Respuesta de(final int status, final String cuerpo) {
            return new Respuesta(status, cuerpo, 0);
        }
    }

    private final HttpServer servidor;
    final List<Peticion> peticiones = new CopyOnWriteArrayList<>();
    private final AtomicReference<Function<Peticion, Respuesta>> manejador =
            new AtomicReference<>(p -> Respuesta.de(200, "{}"));

    ServidorNotificacionesFalso() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", this::atender);
        servidor.start();
    }

    String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    void responder(final int status, final String cuerpo) {
        manejador.set(p -> Respuesta.de(status, cuerpo));
    }

    void responder(final Function<Peticion, Respuesta> funcion) {
        manejador.set(funcion);
    }

    private void atender(final HttpExchange intercambio) throws IOException {
        final String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        final Peticion peticion = new Peticion(intercambio.getRequestMethod(), intercambio.getRequestURI().getRawPath(),
                intercambio.getRequestURI().getRawQuery(), Map.copyOf(intercambio.getRequestHeaders()), cuerpo);
        peticiones.add(peticion);
        final Respuesta respuesta = manejador.get().apply(peticion);
        if (respuesta.demoraMs() > 0) {
            try {
                Thread.sleep(respuesta.demoraMs());
            } catch (final InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        }
        final byte[] bytes = respuesta.cuerpo().getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().add("Content-Type", "application/json");
        try {
            intercambio.sendResponseHeaders(respuesta.status(), bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                intercambio.getResponseBody().write(bytes);
            }
        } catch (final IOException _) {
            // el cliente ya cerro la conexion (tiempo de espera agotado)
        } finally {
            intercambio.close();
        }
    }

    @Override
    public void close() {
        servidor.stop(0);
    }
}
