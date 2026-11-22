package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact.ContactoDestinatario;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact.ContactoDestinatarioResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.DestinatarioRef;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionNoEntregableException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionPersistida;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.verificacion.MarcadorDinamicoProvider;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.NotificacionesServiceProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicioNotificacionChannelSenderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ACEPTADA = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"estado\":\"PENDIENTE\","
            + "\"idempotencyKey\":\"k\",\"duplicada\":false}";

    private ServidorNotificacionesFalso servidor;
    private CircuitBreaker breaker;
    private SimpleMeterRegistry metricas;
    private final UUID usuario = UUID.randomUUID();
    private List<ContactoDestinatario> contactos;
    private Map<String, String> dinamicos = Map.of();
    private ListAppender<ILoggingEvent> registro;

    @BeforeEach
    void preparar() throws Exception {
        servidor = new ServidorNotificacionesFalso();
        breaker = CircuitBreaker.of("sender-" + UUID.randomUUID(), CircuitBreakerConfig.ofDefaults());
        metricas = new SimpleMeterRegistry();
        contactos = List.of(new ContactoDestinatario(usuario, "ana.gomez@uco.edu.co", true));
        registro = new ListAppender<>();
        registro.start();
        ((Logger) LoggerFactory.getLogger(ServicioNotificacionChannelSender.class)).addAppender(registro);
    }

    @AfterEach
    void cerrar() {
        servidor.close();
        ((Logger) LoggerFactory.getLogger(ServicioNotificacionChannelSender.class)).detachAppender(registro);
    }

    private ServicioNotificacionChannelSender sender(final boolean exigirConfirmado, final Duration lectura) {
        final NotificacionesServiceProperties props = new NotificacionesServiceProperties(
                servidor.url(), "clave", Duration.ofSeconds(1), lectura, "asistencias", "es", exigirConfirmado, "");
        final NotificacionesServiceClient cliente = new NotificacionesServiceClient(
                HttpClient.newHttpClient(), props, breaker, MAPPER);
        final ContactoDestinatarioResolver resolver = ids -> contactos.stream().filter(c -> ids.contains(c.idUsuario())).toList();
        final MarcadorDinamicoProvider proveedor = (tipo, ids) -> dinamicos;
        return new ServicioNotificacionChannelSender(cliente, resolver, proveedor, props, metricas);
    }

    private NotificacionPersistida notificacion(final String tipo, final UUID... usuarios) {
        final List<DestinatarioRef> refs = new ArrayList<>();
        for (final UUID id : usuarios) {
            refs.add(new DestinatarioRef("ESTUDIANTE", id));
        }
        return new NotificacionPersistida(UUID.randomUUID(), tipo, "Clase cancelada", "El docente cancelo la clase.",
                refs, Map.of("estudiante", "Ana Gomez", "grupo", "G01"), 1, Instant.now());
    }

    private JsonNode cuerpo(final int indice) throws Exception {
        return MAPPER.readTree(servidor.peticiones.get(indice).cuerpo());
    }

    @Test
    void entrega_el_aviso_con_la_forma_del_contrato_y_la_clave_de_idempotencia_del_outbox() throws Exception {
        servidor.responder(202, ACEPTADA);
        dinamicos = Map.of("enlace", "http://localhost:4200/verificar-correo#token=abc");
        final NotificacionPersistida n = notificacion("VERIFICACION_CORREO", usuario);

        sender(false, Duration.ofSeconds(5)).enviar(n);

        assertEquals(1, servidor.peticiones.size());
        assertEquals("/api/v1/notificaciones", servidor.peticiones.getFirst().ruta());
        assertEquals("clave", servidor.peticiones.getFirst().cabecera("X-Api-Key"));
        final JsonNode body = cuerpo(0);
        assertEquals("asistencias", body.get("aplicacion").asText());
        assertEquals(n.id().toString(), body.get("idempotencyKey").asText());
        assertEquals("VERIFICACION_CORREO", body.get("tipo").asText());
        assertEquals("es", body.get("idioma").asText());
        assertEquals("Clase cancelada", body.get("asunto").asText());
        assertEquals("El docente cancelo la clase.", body.get("mensaje").asText());
        assertEquals("ana.gomez@uco.edu.co", body.get("destinatarios").get(0).get("correo").asText());
        assertEquals("Ana Gomez", body.get("datos").get("estudiante").asText());
        assertEquals("http://localhost:4200/verificar-correo#token=abc", body.get("datos").get("enlace").asText());
        assertEquals(1.0, metricas.counter("notification.service.send.total", "outcome", "accepted").count());
    }

    @Test
    void una_respuesta_200_de_clave_repetida_se_da_por_entregada() throws Exception {
        servidor.responder(200, ACEPTADA.replace("false", "true"));

        sender(false, Duration.ofSeconds(5)).enviar(notificacion("SESION_CANCELADA", usuario));

        assertEquals(1.0, metricas.counter("notification.service.send.total", "outcome", "duplicate").count());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 409, 413, 422})
    void un_4xx_es_fallo_definitivo_con_el_codigo_del_servicio_y_sin_su_mensaje(final int status) {
        servidor.responder(status, "{\"code\":\"TIPO_NO_SOPORTADO\",\"message\":\"correo ana@uco.edu.co invalido\"}");

        final NotificacionNoEntregableException e = assertThrows(NotificacionNoEntregableException.class,
                () -> sender(false, Duration.ofSeconds(5)).enviar(notificacion("TIPO_RETIRADO", usuario)));

        assertTrue(e.getMessage().contains("TIPO_NO_SOPORTADO"));
        assertFalse(e.getMessage().contains("ana@uco.edu.co"));
        assertEquals(1.0, metricas.counter("notification.service.send.total", "outcome", "permanent").count());
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503, 408, 429})
    void un_5xx_o_un_408_429_es_reintentable(final int status) {
        servidor.responder(status, "{\"code\":\"ERROR_INTERNO\",\"message\":\"x\"}");

        final RuntimeException e = assertThrows(RuntimeException.class,
                () -> sender(false, Duration.ofSeconds(5)).enviar(notificacion("SESION_CANCELADA", usuario)));

        assertInstanceOf(ServicioNotificacionesException.class, e);
        assertEquals(1.0, metricas.counter("notification.service.send.total", "outcome", "retry").count());
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403})
    void un_401_o_403_es_error_de_configuracion_reintentable_registrado_como_error(final int status) {
        servidor.responder(status, "{\"code\":\"NO_AUTORIZADO\",\"message\":\"x\"}");

        assertThrows(ServicioNotificacionesException.class,
                () -> sender(false, Duration.ofSeconds(5)).enviar(notificacion("SESION_CANCELADA", usuario)));

        assertEquals(1.0, metricas.counter("notification.service.send.total", "outcome", "config_error").count());
        assertTrue(registro.list.stream().anyMatch(ev -> ev.getLevel() == Level.ERROR
                && ev.getFormattedMessage().contains("NO_AUTORIZADO")));
        assertFalse(registro.list.stream().anyMatch(ev -> ev.getFormattedMessage().contains("clave")));
    }

    @Test
    void el_tiempo_de_espera_agotado_es_reintentable() {
        servidor.responder(p -> new ServidorNotificacionesFalso.Respuesta(202, ACEPTADA, 1500));

        assertThrows(ServicioNotificacionesNoDisponibleException.class,
                () -> sender(false, Duration.ofMillis(300)).enviar(notificacion("SESION_CANCELADA", usuario)));
    }

    @Test
    void los_reintentos_reusan_la_misma_clave_de_idempotencia() throws Exception {
        servidor.responder(503, "{\"code\":\"ERROR_INTERNO\",\"message\":\"x\"}");
        final ServicioNotificacionChannelSender sender = sender(false, Duration.ofSeconds(5));
        final NotificacionPersistida n = notificacion("SESION_CANCELADA", usuario);

        assertThrows(RuntimeException.class, () -> sender.enviar(n));
        servidor.responder(202, ACEPTADA);
        sender.enviar(n);

        assertEquals(2, servidor.peticiones.size());
        assertEquals(cuerpo(0).get("idempotencyKey").asText(), cuerpo(1).get("idempotencyKey").asText());
        assertEquals(n.id().toString(), cuerpo(1).get("idempotencyKey").asText());
    }

    @Test
    void sin_destinatarios_con_correo_valido_es_definitivo_y_no_llama_al_servicio() {
        contactos = List.of(new ContactoDestinatario(usuario, "no es un correo", true));

        assertThrows(NotificacionNoEntregableException.class,
                () -> sender(false, Duration.ofSeconds(5)).enviar(notificacion("SESION_CANCELADA", usuario)));

        assertTrue(servidor.peticiones.isEmpty());
    }

    @Test
    void con_correo_confirmado_obligatorio_se_omiten_los_no_confirmados() throws Exception {
        final UUID otro = UUID.randomUUID();
        contactos = List.of(new ContactoDestinatario(usuario, "ana.gomez@uco.edu.co", false),
                new ContactoDestinatario(otro, "luis.perez@uco.edu.co", true));
        servidor.responder(202, ACEPTADA);

        sender(true, Duration.ofSeconds(5)).enviar(notificacion("SESION_CANCELADA", usuario, otro));

        final JsonNode destinatarios = cuerpo(0).get("destinatarios");
        assertEquals(1, destinatarios.size());
        assertEquals("luis.perez@uco.edu.co", destinatarios.get(0).get("correo").asText());
    }

    @ParameterizedTest
    @ValueSource(strings = {Notificacion.TIPO_VERIFICACION_CORREO, Notificacion.TIPO_ACTIVACION_CUENTA})
    void verificacion_y_activacion_llegan_a_correos_sin_confirmar(final String tipo) throws Exception {
        contactos = List.of(new ContactoDestinatario(usuario, "ana.gomez@uco.edu.co", false));
        servidor.responder(202, ACEPTADA);

        sender(true, Duration.ofSeconds(5)).enviar(notificacion(tipo, usuario));

        assertEquals(1, cuerpo(0).get("destinatarios").size());
    }

    @Test
    void los_destinatarios_repetidos_se_envian_una_vez_y_se_parten_en_bloques_de_200() throws Exception {
        final List<ContactoDestinatario> muchos = new ArrayList<>();
        final UUID[] ids = new UUID[201];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = UUID.randomUUID();
            muchos.add(new ContactoDestinatario(ids[i], "u" + i + "@uco.edu.co", true));
        }
        contactos = muchos;
        servidor.responder(202, ACEPTADA);
        final NotificacionPersistida n = notificacion("CIERRE_MASIVO_PERIODO_EJECUTADO", ids);

        sender(false, Duration.ofSeconds(5)).enviar(n);

        assertEquals(2, servidor.peticiones.size());
        assertEquals(200, cuerpo(0).get("destinatarios").size());
        assertEquals(1, cuerpo(1).get("destinatarios").size());
        assertEquals(n.id().toString(), cuerpo(0).get("idempotencyKey").asText());
        assertEquals(n.id() + "-2", cuerpo(1).get("idempotencyKey").asText());
    }

    @Test
    void un_marcador_dinamico_no_entregable_se_propaga_sin_llamar_al_servicio() {
        final NotificacionesServiceProperties props = new NotificacionesServiceProperties(
                servidor.url(), "clave", Duration.ofSeconds(1), Duration.ofSeconds(1), "asistencias", "es", false, "");
        final ServicioNotificacionChannelSender sender = new ServicioNotificacionChannelSender(
                new NotificacionesServiceClient(HttpClient.newHttpClient(), props, breaker, MAPPER),
                ids -> contactos,
                (tipo, ids) -> {
                    throw new NotificacionNoEntregableException("usuario ya verificado");
                },
                props, metricas);

        assertThrows(NotificacionNoEntregableException.class,
                () -> sender.enviar(notificacion("VERIFICACION_CORREO", usuario)));
        assertTrue(servidor.peticiones.isEmpty());
    }

    @Test
    void disponible_refleja_el_estado_del_circuit_breaker() {
        final ServicioNotificacionChannelSender sender = sender(false, Duration.ofSeconds(1));
        assertTrue(sender.disponible());

        breaker.transitionToOpenState();

        assertFalse(sender.disponible());
    }

    @Test
    void los_datos_con_clave_invalida_no_viajan() throws Exception {
        servidor.responder(202, ACEPTADA);
        final Map<String, String> datos = new java.util.HashMap<>();
        datos.put("ok", "valor");
        datos.put("clave invalida", "x");
        final NotificacionPersistida n = new NotificacionPersistida(UUID.randomUUID(), "SESION_CANCELADA", "a", "b",
                List.of(new DestinatarioRef("ESTUDIANTE", usuario)), datos, 1, Instant.now());

        sender(false, Duration.ofSeconds(5)).enviar(n);

        final JsonNode enviados = cuerpo(0).get("datos");
        assertEquals(1, enviados.size());
        assertEquals("valor", enviados.get("ok").asText());
    }
}
