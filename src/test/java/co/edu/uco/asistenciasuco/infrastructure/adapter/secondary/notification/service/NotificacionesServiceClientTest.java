package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.service;

import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.NotificacionesServiceProperties;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificacionesServiceClientTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ServidorNotificacionesFalso servidor;
    private CircuitBreaker breaker;

    @BeforeEach
    void preparar() throws Exception {
        servidor = new ServidorNotificacionesFalso();
        breaker = CircuitBreaker.of("prueba-" + UUID.randomUUID(), CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(4).minimumNumberOfCalls(4).failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(5)).build());
    }

    @AfterEach
    void cerrar() {
        servidor.close();
        CorrelationIdContext.clear();
    }

    private NotificacionesServiceClient cliente(final String url, final Duration lectura) {
        final NotificacionesServiceProperties props = new NotificacionesServiceProperties(
                url, "clave-de-prueba", Duration.ofSeconds(1), lectura, "asistencias", "es", false, "");
        return new NotificacionesServiceClient(
                HttpClient.newBuilder().connectTimeout(props.connectTimeout()).build(), props, breaker, MAPPER);
    }

    @Test
    void envia_clave_api_correlacion_y_cuerpo_json() throws Exception {
        final UUID correlacion = UUID.randomUUID();
        CorrelationIdContext.set(correlacion);
        servidor.responder(202, "{\"id\":\"x\",\"estado\":\"PENDIENTE\",\"duplicada\":false}");

        final RespuestaServicio respuesta = cliente(servidor.url(), Duration.ofSeconds(5))
                .post("/api/v1/notificaciones", MAPPER.createObjectNode().put("tipo", "SESION_CANCELADA"));

        assertEquals(202, respuesta.status());
        assertEquals("PENDIENTE", respuesta.cuerpo().get("estado").asText());
        final var peticion = servidor.peticiones.getFirst();
        assertEquals("POST", peticion.metodo());
        assertEquals("/api/v1/notificaciones", peticion.ruta());
        assertEquals("clave-de-prueba", peticion.cabecera("X-Api-Key"));
        assertEquals(correlacion.toString(), peticion.cabecera("X-Correlation-Id"));
        assertTrue(peticion.cabecera("Content-Type").startsWith("application/json"));
        assertEquals("SESION_CANCELADA", MAPPER.readTree(peticion.cuerpo()).get("tipo").asText());
    }

    @Test
    void get_codifica_los_parametros_de_consulta() {
        servidor.responder(200, "{\"elementos\":[]}");

        cliente(servidor.url(), Duration.ofSeconds(5))
                .get("/api/v1/notificaciones", Map.of("tipo", "A B&C"));

        assertEquals("tipo=A%20B%26C", servidor.peticiones.getFirst().consulta());
        assertEquals("GET", servidor.peticiones.getFirst().metodo());
    }

    @Test
    void una_respuesta_4xx_lanza_la_excepcion_con_su_codigo_y_no_cuenta_como_falla_del_breaker() {
        servidor.responder(404, "{\"code\":\"TIPO_NO_ENCONTRADO\",\"message\":\"No existe\",\"correlationId\":\"c\"}");
        final NotificacionesServiceClient cliente = cliente(servidor.url(), Duration.ofSeconds(5));

        for (int i = 0; i < 6; i++) {
            final ServicioNotificacionesException e = assertThrows(ServicioNotificacionesException.class,
                    () -> cliente.get("/api/v1/plantillas", Map.of()));
            assertEquals(404, e.status());
            assertEquals("TIPO_NO_ENCONTRADO", e.code());
            assertEquals("No existe", e.mensajeServicio());
        }
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
        assertEquals(6, servidor.peticiones.size());
    }

    @Test
    void un_cuerpo_de_error_ilegible_deja_el_codigo_vacio() {
        servidor.responder(500, "<html>boom</html>");

        final ServicioNotificacionesException e = assertThrows(ServicioNotificacionesException.class,
                () -> cliente(servidor.url(), Duration.ofSeconds(5)).get("/api/v1/x", Map.of()));

        assertEquals(500, e.status());
        assertEquals("", e.code());
    }

    @Test
    void el_tiempo_de_espera_agotado_es_no_disponible() {
        servidor.responder(p -> new ServidorNotificacionesFalso.Respuesta(202, "{}", 1500));

        assertThrows(ServicioNotificacionesNoDisponibleException.class,
                () -> cliente(servidor.url(), Duration.ofMillis(300)).post("/api/v1/notificaciones", MAPPER.createObjectNode()));
    }

    @Test
    void la_conexion_rechazada_es_no_disponible() {
        final String urlCerrada = servidor.url();
        servidor.close();

        assertThrows(ServicioNotificacionesNoDisponibleException.class,
                () -> cliente(urlCerrada, Duration.ofSeconds(2)).get("/api/v1/plantillas", Map.of()));
    }

    @Test
    void un_segmento_con_barra_se_codifica_y_no_sale_de_la_ruta_de_plantillas() {
        servidor.responder(200, "{}");

        cliente(servidor.url(), Duration.ofSeconds(5))
                .post("/api/v1/plantillas/" + NotificacionesServiceClient.codificar("../admin") + "/versiones", null);

        assertEquals("/api/v1/plantillas/..%2Fadmin/versiones", servidor.peticiones.getFirst().ruta());
    }

    @Test
    void rechaza_rutas_fuera_de_la_api_o_con_segmentos_relativos_sin_llamar_al_servicio() {
        final NotificacionesServiceClient cliente = cliente(servidor.url(), Duration.ofSeconds(5));

        for (final String ruta : new String[] {
                "/actuator/env", "/api/v1/../actuator", "/api/v1/plantillas/./x", "/api/v1/plantillas/..",
                "/api/v1//x", "/api/v1/x@otro-host", "/api/v1/x?y=1", "api/v1/x", "/api/v1"}) {
            assertThrows(IllegalArgumentException.class, () -> cliente.get(ruta, Map.of()), ruta);
        }
        assertThrows(IllegalArgumentException.class, () -> cliente.get(null, Map.of()));
        assertTrue(servidor.peticiones.isEmpty());
    }

    @Test
    void sin_url_configurada_no_se_intenta_la_conexion() {
        final NotificacionesServiceClient cliente = cliente("", Duration.ofSeconds(1));

        assertFalse(cliente.configurado());
        assertThrows(ServicioNotificacionesNoDisponibleException.class, () -> cliente.get("/api/v1/x", Map.of()));
        assertTrue(servidor.peticiones.isEmpty());
    }

    @Test
    void con_el_breaker_abierto_falla_rapido_sin_llamar_al_servicio() {
        servidor.responder(503, "{\"code\":\"ERROR_INTERNO\",\"message\":\"x\"}");
        final NotificacionesServiceClient cliente = cliente(servidor.url(), Duration.ofSeconds(5));
        for (int i = 0; i < 4; i++) {
            assertThrows(ServicioNotificacionesException.class, () -> cliente.get("/api/v1/x", Map.of()));
        }
        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());
        assertFalse(cliente.disponible());
        final int llamadas = servidor.peticiones.size();

        final ServicioNotificacionesNoDisponibleException e = assertThrows(ServicioNotificacionesNoDisponibleException.class,
                () -> cliente.get("/api/v1/x", Map.of()));

        assertTrue(e.getMessage().contains("circuit"));
        assertEquals(llamadas, servidor.peticiones.size());
    }

    @Test
    void readiness_es_verdadero_solo_con_estado_up() {
        servidor.responder(200, "{\"status\":\"UP\"}");
        assertTrue(cliente(servidor.url(), Duration.ofSeconds(2)).readiness());
        assertEquals("/actuator/health/readiness", servidor.peticiones.getFirst().ruta());
        assertNull(servidor.peticiones.getFirst().cabecera("X-Api-Key"));

        servidor.responder(503, "{\"status\":\"DOWN\"}");
        assertFalse(cliente(servidor.url(), Duration.ofSeconds(2)).readiness());
    }

    @Test
    void readiness_no_afecta_al_breaker_y_es_falso_si_no_responde() {
        final String urlCerrada = servidor.url();
        servidor.close();
        final NotificacionesServiceClient cliente = cliente(urlCerrada, Duration.ofSeconds(1));
        for (int i = 0; i < 6; i++) {
            assertFalse(cliente.readiness());
        }
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
    }
}
