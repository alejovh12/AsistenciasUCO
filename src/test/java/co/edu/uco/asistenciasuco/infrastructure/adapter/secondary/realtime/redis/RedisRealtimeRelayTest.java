package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.redis;

import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse.ReactorRealtimeAdapter;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * REDIS-001 (TD-003): el relevo publica en el canal Redis; cada replica entrega a sus clientes SSE lo que recibe del canal
 * (incluida la de origen, sin duplicados); con Redis caido entrega solo en la replica local y nunca lanza al caso de uso.
 */
class RedisRealtimeRelayTest {

    private static final String CANAL = "asistencias-uco:realtime:v1";
    private static final UUID DESTINATARIO = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final RedisRealtimeCodec codec = new RedisRealtimeCodec(JsonMapper.builder().build(), "replica-a");
    private final RedisRealtimeCodec codecOtraReplica = new RedisRealtimeCodec(JsonMapper.builder().build(), "replica-b");
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private static RealtimeEvent evento() {
        return RealtimeEvent.paraDestinatarios("ALERTA_INASISTENCIA",
                Map.of("grupo", "g-1", "totalPresentes", 12), Set.of(DESTINATARIO));
    }

    @Test
    void el_codec_conserva_el_evento_y_sus_destinatarios() {
        final RealtimeEvent original = new RealtimeEvent(UUID.randomUUID(), "TOMA_ABIERTA", Instant.parse("2026-11-25T12:00:00Z"),
                "corr-1", "trace-1", "span-1", Map.of("grupo", "g-1", "toma", "t-1"), Set.of(DESTINATARIO));

        final RealtimeEvent leido = codec.leer(codec.escribir(original));

        assertEquals(original, leido);
    }

    @Test
    void el_codec_descarta_otra_version_del_formato() {
        assertNull(codec.leer("{\"v\":2,\"eventId\":\"" + UUID.randomUUID() + "\",\"type\":\"X\",\"occurredAt\":\"2026-11-25T12:00:00Z\"}"));
        final RealtimeEvent sinDestinatarios = codec.leer("{\"v\":1,\"eventId\":\"" + UUID.randomUUID()
                + "\",\"type\":\"X\",\"occurredAt\":\"2026-11-25T12:00:00Z\",\"payload\":{}}");
        assertEquals(Set.of(), sinDestinatarios.destinatariosUsuario());
    }

    @Test
    void publicar_envia_al_canal_con_la_correlacion_del_origen_y_entrega_localmente() {
        final ReactorRealtimeAdapter local = new ReactorRealtimeAdapter(registry);
        final RedisRealtimeRelayAdapter relevo = new RedisRealtimeRelayAdapter(redis, CANAL, codec, local, registry);
        CorrelationIdContext.set("corr-origen");
        final ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);

        relevo.publish(evento());

        verify(redis).convertAndSend(eq(CANAL), mensaje.capture());
        final RealtimeEvent enviado = codec.leer(mensaje.getValue());
        assertEquals("corr-origen", enviado.correlationId());
        assertEquals(Set.of(DESTINATARIO), enviado.destinatariosUsuario());
        assertEquals(1.0, registry.get("realtime.relay.published").counter().count());
        assertEquals(1.0, registry.get("realtime.events.no_subscribers").counter().count(),
                "la replica de origen lo entrega a su sink local (aqui sin clientes conectados)");
    }

    @Test
    void la_suscripcion_entrega_a_los_clientes_sse_de_la_replica() {
        final ReactorRealtimeAdapter local = new ReactorRealtimeAdapter(registry);
        final RedisRealtimeSuscriptor suscriptor = new RedisRealtimeSuscriptor(codec, local);
        final RealtimeEvent original = evento().withObservabilityContext("corr-remota", "trace", "span");
        final byte[] cuerpo = codecOtraReplica.escribir(original).getBytes(StandardCharsets.UTF_8);

        StepVerifier.create(local.events())
                .then(() -> suscriptor.onMessage(new DefaultMessage(CANAL.getBytes(StandardCharsets.UTF_8), cuerpo), null))
                .assertNext(recibido -> {
                    assertEquals(original.eventId(), recibido.eventId());
                    assertEquals("corr-remota", recibido.correlationId(), "no se reemplaza la correlacion del origen");
                    assertEquals(Set.of(DESTINATARIO), recibido.destinatariosUsuario());
                })
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void un_mensaje_ilegible_o_de_otra_version_se_descarta_sin_cortar_la_suscripcion() {
        final ReactorRealtimeAdapter local = new ReactorRealtimeAdapter(registry);
        final RedisRealtimeSuscriptor suscriptor = new RedisRealtimeSuscriptor(codec, local);
        final byte[] canal = CANAL.getBytes(StandardCharsets.UTF_8);

        assertDoesNotThrow(() -> suscriptor.onMessage(new DefaultMessage(canal, "{roto".getBytes(StandardCharsets.UTF_8)), null));
        assertDoesNotThrow(() -> suscriptor.onMessage(new DefaultMessage(canal,
                ("{\"v\":9,\"eventId\":\"" + UUID.randomUUID() + "\",\"type\":\"X\",\"occurredAt\":\"2026-11-25T12:00:00Z\"}")
                        .getBytes(StandardCharsets.UTF_8)), null));
    }

    @Test
    void con_redis_caido_entrega_solo_en_esta_replica_y_no_lanza() {
        final ReactorRealtimeAdapter local = new ReactorRealtimeAdapter(registry);
        final RedisRealtimeRelayAdapter relevo = new RedisRealtimeRelayAdapter(redis, CANAL, codec, local, registry);
        doThrow(new RedisConnectionFailureException("caido")).when(redis).convertAndSend(anyString(), anyString());
        final RealtimeEvent original = evento();

        StepVerifier.create(local.events())
                .then(() -> relevo.publish(original))
                .assertNext(recibido -> assertEquals(original.eventId(), recibido.eventId()))
                .thenCancel()
                .verify(Duration.ofSeconds(2));
        assertEquals(1.0, registry.get("realtime.relay.fallback").counter().count());
    }

    @Test
    void un_evento_nulo_se_cuenta_como_descartado_y_no_viaja() {
        final ReactorRealtimeAdapter local = new ReactorRealtimeAdapter(registry);
        final RedisRealtimeRelayAdapter relevo = new RedisRealtimeRelayAdapter(redis, CANAL, codec, local, registry);

        relevo.publish(null);
        local.entregarRelevado(null);

        verify(redis, never()).convertAndSend(anyString(), anyString());
        assertEquals(2.0, registry.get("realtime.events.dropped").counter().count());
    }

    @Test
    void un_hecho_de_negocio_se_cuenta_una_sola_vez_en_el_origen() {
        final ReactorRealtimeAdapter local = new ReactorRealtimeAdapter(registry);
        final RedisRealtimeRelayAdapter relevo = new RedisRealtimeRelayAdapter(redis, CANAL, codec, local, registry);
        final RedisRealtimeSuscriptor suscriptor = new RedisRealtimeSuscriptor(codec, local);
        final ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);

        relevo.publish(RealtimeEvent.of("TOMA_ABIERTA", Map.of("grupo", "g-1")));
        verify(redis).convertAndSend(eq(CANAL), mensaje.capture());
        suscriptor.onMessage(new DefaultMessage(CANAL.getBytes(StandardCharsets.UTF_8),
                mensaje.getValue().getBytes(StandardCharsets.UTF_8)), null);

        assertEquals(1.0, registry.get("asistencias.negocio.eventos").tag("tipo", "TOMA_ABIERTA").counter().count());
    }

    @Test
    void una_falla_al_preparar_el_evento_no_llega_al_caso_de_uso() {
        final ReactorRealtimeAdapter local = mock(ReactorRealtimeAdapter.class);
        when(local.prepararParaRelevo(org.mockito.ArgumentMatchers.any())).thenThrow(new IllegalStateException("x"));
        final RedisRealtimeRelayAdapter relevo = new RedisRealtimeRelayAdapter(redis, CANAL, codec, local, registry);

        assertDoesNotThrow(() -> relevo.publish(evento()));
        verify(redis, never()).convertAndSend(anyString(), anyString());
    }
    @Test
    void el_mensaje_propio_que_vuelve_por_la_suscripcion_no_se_entrega_dos_veces() {
        final ReactorRealtimeAdapter local = new ReactorRealtimeAdapter(registry);
        final RedisRealtimeRelayAdapter relevo = new RedisRealtimeRelayAdapter(redis, CANAL, codec, local, registry);
        final RedisRealtimeSuscriptor suscriptor = new RedisRealtimeSuscriptor(codec, local);
        final ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        final java.util.List<RealtimeEvent> recibidos = new java.util.concurrent.CopyOnWriteArrayList<>();
        local.events().subscribe(recibidos::add);

        relevo.publish(evento());
        verify(redis).convertAndSend(eq(CANAL), mensaje.capture());
        suscriptor.onMessage(new DefaultMessage(CANAL.getBytes(StandardCharsets.UTF_8),
                mensaje.getValue().getBytes(StandardCharsets.UTF_8)), null);

        assertEquals(1, recibidos.size(), "se entrega al publicar y el eco propio se descarta");
    }

    @Test
    void un_timeout_despues_de_que_redis_acepto_el_mensaje_no_duplica_la_entrega() {
        final ReactorRealtimeAdapter local = new ReactorRealtimeAdapter(registry);
        final RedisRealtimeRelayAdapter relevo = new RedisRealtimeRelayAdapter(redis, CANAL, codec, local, registry);
        final RedisRealtimeSuscriptor suscriptor = new RedisRealtimeSuscriptor(codec, local);
        final ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        doThrow(new org.springframework.dao.QueryTimeoutException("timeout")).when(redis).convertAndSend(eq(CANAL), mensaje.capture());
        final java.util.List<RealtimeEvent> recibidos = new java.util.concurrent.CopyOnWriteArrayList<>();
        local.events().subscribe(recibidos::add);

        relevo.publish(evento());
        // Redis si alcanzo a aceptar el mensaje y la suscripcion propia lo recibe despues del timeout.
        suscriptor.onMessage(new DefaultMessage(CANAL.getBytes(StandardCharsets.UTF_8),
                mensaje.getValue().getBytes(StandardCharsets.UTF_8)), null);

        assertEquals(1, recibidos.size());
        assertEquals(1.0, registry.get("realtime.relay.fallback").counter().count());
    }
}
