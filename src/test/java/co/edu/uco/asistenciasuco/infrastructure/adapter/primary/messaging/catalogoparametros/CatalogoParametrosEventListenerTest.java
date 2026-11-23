package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.messaging.catalogoparametros;

import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarparametros.primaryports.SincronizarParametrosCatalogoInputPort;
import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarparametros.primaryports.SincronizarParametrosCatalogoInputPort.AvisoCambioParametro;
import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarparametros.primaryports.SincronizarParametrosCatalogoInputPort.ResumenSincronizacion;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.ConsumerBuilder;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
class CatalogoParametrosEventListenerTest {

    private PulsarClient pulsar;
    private ConsumerBuilder<String> builder;
    private SincronizarParametrosCatalogoInputPort sincronizacion;
    private SimpleMeterRegistry metricas;

    @BeforeEach
    void preparar() {
        pulsar = mock(PulsarClient.class);
        builder = mock(ConsumerBuilder.class, RETURNS_SELF);
        when(pulsar.newConsumer(any(org.apache.pulsar.client.api.Schema.class))).thenReturn(builder);
        sincronizacion = mock(SincronizarParametrosCatalogoInputPort.class);
        metricas = new SimpleMeterRegistry();
    }

    private CatalogoParametrosEventListener listener(final boolean reconciliar) {
        return new CatalogoParametrosEventListener(pulsar, sincronizacion, JsonMapper.builder().build(), "persistent://public/default/t-svc",
                "sub", reconciliar, metricas, Duration.ofMillis(5));
    }

    private static String fixture() throws Exception {
        try (var in = CatalogoParametrosEventListenerTest.class.getResourceAsStream("/fixtures/catalogo-parametros/evento-ejemplo.json")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private double eventos(final String resultado) {
        final var c = metricas.find(CatalogoParametrosEventListener.METRICA_EVENTOS).tag("resultado", resultado).counter();
        return c == null ? 0 : c.count();
    }

    @Test
    void el_evento_real_se_entrega_al_puerto_con_sus_campos_y_se_confirma() throws Exception {
        when(sincronizacion.procesarAviso(any())).thenReturn(new ResumenSincronizacion(1, 1, 0, 0, 0, 0, 0));
        final var l = listener(false);

        assertTrue(l.procesar(fixture()));

        final var captor = forClass(AvisoCambioParametro.class);
        verify(sincronizacion).procesarAviso(captor.capture());
        assertEquals(new AvisoCambioParametro("355f30cf-be31-4833-be7d-5667b0345601", "MetadataUpdated", "RECLAMO.ESTADO_RETIRADO",
                "0d24a3a4-8c9d-4ac3-9b1a-a97ef16ea186", "00000000-0000-0000-0000-000000000011",
                Instant.parse("2026-11-23T03:50:12.246789882Z")), captor.getValue());
        assertEquals(1, eventos("recibido"));
        assertEquals(1, eventos("procesado"));
        assertEquals(1, metricas.get(CatalogoParametrosEventListener.METRICA_VALORES).tag("resultado", "aplicado").counter().count());
        assertTrue(l.ultimaSincronizacionEpochSegundos() > 0);
    }

    @Test
    void un_fallo_del_puerto_no_confirma_y_cuenta_como_fallido() throws Exception {
        when(sincronizacion.procesarAviso(any())).thenThrow(new IllegalStateException("catalogo caido"));

        assertFalse(listener(false).procesar(fixture()));

        assertEquals(1, eventos("fallido"));
        assertEquals(0, eventos("procesado"));
    }

    @Test
    void eventos_ilegibles_vacios_ajenos_o_sin_tipo_se_descartan_con_ack_sin_llamar_al_puerto() {
        final var l = listener(false);

        assertTrue(l.procesar(null));
        assertTrue(l.procesar("  "));
        assertTrue(l.procesar("{no es json"));
        assertTrue(l.procesar("[1,2]"));
        assertTrue(l.procesar("{\"source\":\"otro-sistema\",\"eventType\":\"ParameterUpdated\"}"));
        assertTrue(l.procesar("{\"source\":\"catalogo-parametros\"}"));

        verifyNoInteractions(sincronizacion);
        assertEquals(6, eventos("descartado"));
    }

    @Test
    void sin_fuente_ni_hora_valida_se_procesa_igual_y_acepta_offset_o_ignora_la_hora_mala() {
        final var l = listener(false);
        when(sincronizacion.procesarAviso(any())).thenReturn(ResumenSincronizacion.vacio());

        assertTrue(l.procesar("{\"eventType\":\"ParameterUpdated\",\"resourceName\":\"A.B\",\"schemaVersion\":4,"
                + "\"occurredAt\":\"2026-11-22T10:00:00+00:00\"}"));
        assertTrue(l.procesar("{\"eventType\":\"ParameterUpdated\",\"resourceName\":\"A.B\",\"occurredAt\":\"ayer\"}"));
        assertTrue(l.procesar("{\"eventType\":\"ParameterUpdated\",\"resourceName\":\"A.B\",\"occurredAt\":\" \"}"));

        final var captor = forClass(AvisoCambioParametro.class);
        verify(sincronizacion, times(3)).procesarAviso(captor.capture());
        assertEquals(Instant.parse("2026-11-22T10:00:00Z"), captor.getAllValues().get(0).ocurridoEn());
        assertEquals(null, captor.getAllValues().get(1).ocurridoEn());
        assertEquals(null, captor.getAllValues().get(2).ocurridoEn());
    }

    @Test
    void recibir_hace_ack_si_se_proceso_y_nack_si_no() {
        final Consumer<String> origen = mock(Consumer.class);
        final Message<String> bueno = mock(Message.class);
        final Message<String> malo = mock(Message.class);
        when(bueno.getValue()).thenReturn("{\"eventType\":\"ParameterUpdated\",\"resourceName\":\"A.B\"}");
        when(malo.getValue()).thenReturn("{\"eventType\":\"ParameterUpdated\",\"resourceName\":\"A.C\"}");
        when(sincronizacion.procesarAviso(any())).thenAnswer(inv -> {
            if ("A.C".equals(((AvisoCambioParametro) inv.getArgument(0)).nombreRecurso())) {
                throw new IllegalStateException("bd caida");
            }
            return ResumenSincronizacion.vacio();
        });
        final var l = listener(false);

        l.recibir(origen, bueno);
        l.recibir(origen, malo);

        verify(origen).acknowledgeAsync(bueno);
        verify(origen).negativeAcknowledge(malo);
        verify(origen, never()).acknowledgeAsync(malo);
    }

    @Test
    void start_se_suscribe_durable_shared_y_stop_cierra_el_consumidor() {
        final Consumer<String> consumidor = mock(Consumer.class);
        when(consumidor.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));
        when(builder.subscribeAsync()).thenReturn(CompletableFuture.completedFuture(consumidor));
        final var l = listener(false);

        assertFalse(l.estaSuscrito());
        l.start();
        l.start();
        assertTrue(l.isRunning());
        assertTrue(l.estaSuscrito());
        verify(builder).topic("persistent://public/default/t-svc");
        verify(builder).subscriptionName("sub");
        verify(builder).subscriptionType(org.apache.pulsar.client.api.SubscriptionType.Shared);
        verify(builder).subscribeAsync();
        verify(sincronizacion, never()).reconciliarTodo();

        l.stop();
        l.stop();
        assertFalse(l.isRunning());
        assertFalse(l.estaSuscrito());
        verify(consumidor).closeAsync();
    }

    @Test
    void si_pulsar_no_responde_reintenta_con_espera_y_no_impide_arrancar() throws Exception {
        final Consumer<String> consumidor = mock(Consumer.class);
        when(builder.subscribeAsync())
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker caido")))
                .thenReturn(CompletableFuture.completedFuture(consumidor));
        final var l = listener(false);

        l.start();
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(5)).until(l::estaSuscrito);
        verify(builder, times(2)).subscribeAsync();
        l.stop();
    }

    @Test
    void una_suscripcion_que_llega_despues_de_stop_se_cierra() {
        final Consumer<String> consumidor = mock(Consumer.class);
        when(consumidor.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));
        final CompletableFuture<Consumer<String>> pendiente = new CompletableFuture<>();
        when(builder.subscribeAsync()).thenReturn(pendiente);
        final var l = listener(false);

        l.start();
        l.stop();
        pendiente.complete(consumidor);

        // stop() y la suscripcion tardia piden el cierre; cerrar dos veces un consumidor de Pulsar es inocuo.
        verify(consumidor, org.mockito.Mockito.atLeastOnce()).closeAsync();
        assertFalse(l.estaSuscrito());
    }

    @Test
    void la_espera_entre_reintentos_se_duplica_hasta_cinco_minutos() {
        final var l = new CatalogoParametrosEventListener(pulsar, sincronizacion, JsonMapper.builder().build(), "t", "s", false, metricas,
                Duration.ofSeconds(15));

        assertEquals(Duration.ofSeconds(15), l.esperaAntesDelIntento(1));
        assertEquals(Duration.ofSeconds(30), l.esperaAntesDelIntento(2));
        assertEquals(Duration.ofMinutes(5), l.esperaAntesDelIntento(10));
        assertEquals(Duration.ofMinutes(5), l.esperaAntesDelIntento(500));
        assertThrows(IllegalArgumentException.class, () -> new CatalogoParametrosEventListener(pulsar, sincronizacion,
                JsonMapper.builder().build(), "t", "s", false, metricas, Duration.ZERO));
    }

    @Test
    void la_reconciliacion_de_arranque_corre_una_vez_y_un_fallo_no_tumba_el_listener() {
        when(builder.subscribeAsync()).thenReturn(new CompletableFuture<>());
        when(sincronizacion.reconciliarTodo()).thenThrow(new IllegalStateException("catalogo caido"))
                .thenReturn(new ResumenSincronizacion(3, 1, 2, 0, 0, 0, 0));
        final var l = listener(true);

        l.start();
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> verify(sincronizacion).reconciliarTodo());
        assertEquals(0, l.ultimaSincronizacionEpochSegundos());

        l.reconciliar();
        assertTrue(l.ultimaSincronizacionEpochSegundos() > 0);
        l.stop();
    }
}
