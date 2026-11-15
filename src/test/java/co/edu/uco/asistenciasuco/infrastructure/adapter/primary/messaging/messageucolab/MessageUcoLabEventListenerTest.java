package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.messaging.messageucolab;

import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarmensajes.primaryports.SincronizarMensajesCatalogoInputPort;
import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarmensajes.primaryports.SincronizarMensajesCatalogoInputPort.AvisoCambioMensaje;
import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarmensajes.primaryports.SincronizarMensajesCatalogoInputPort.ResumenSincronizacion;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.ConsumerBuilder;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** INT-003: avisos de MessageUcoLab por Pulsar -> puerto de sincronizacion, con confirmacion o reintento. */
class MessageUcoLabEventListenerTest {

    private static final String TOPIC = "persistent://public/default/message-events-svc-1";
    private final PulsarClient client = mock(PulsarClient.class);
    private final SincronizarMensajesCatalogoInputPort puerto = mock(SincronizarMensajesCatalogoInputPort.class);

    private MessageUcoLabEventListener listener(final boolean reconciliar) {
        return new MessageUcoLabEventListener(client, puerto, JsonMapper.builder().build(), TOPIC, "asistencias", reconciliar);
    }

    @Test
    void aviso_valido_llega_al_puerto_con_correlacion_y_se_confirma() {
        final AtomicReference<Object> correlacion = new AtomicReference<>();
        when(puerto.procesarAviso(any())).thenAnswer(invocacion -> {
            correlacion.set(CorrelationIdContext.get());
            return ResumenSincronizacion.vacio();
        });

        assertTrue(listener(false).procesar(
                "{\"eventId\":\"e1\",\"eventType\":\"MessageUpdated\",\"messageCode\":\"MSG_VAL001\",\"schemaVersion\":1}"));

        verify(puerto).procesarAviso(new AvisoCambioMensaje("e1", "MessageUpdated", "MSG_VAL001"));
        assertNotNull(correlacion.get());
        assertNull(CorrelationIdContext.get());
    }

    @Test
    void campos_ausentes_llegan_nulos() {
        when(puerto.procesarAviso(any())).thenReturn(ResumenSincronizacion.vacio());
        assertTrue(listener(false).procesar("{\"eventType\":\"MessagesChanged\"}"));
        verify(puerto).procesarAviso(new AvisoCambioMensaje(null, "MessagesChanged", null));
    }

    @Test
    void aviso_ilegible_se_confirma_y_se_descarta() {
        final MessageUcoLabEventListener listener = listener(false);
        assertTrue(listener.procesar("no es json"));
        assertTrue(listener.procesar("[1,2]"));
        assertTrue(listener.procesar(""));
        assertTrue(listener.procesar(null));
        verifyNoInteractions(puerto);
    }

    @Test
    void fallo_al_procesar_pide_reintento() {
        when(puerto.procesarAviso(any())).thenThrow(new IllegalStateException("BD caida"));
        assertFalse(listener(false).procesar("{\"eventType\":\"MessageUpdated\",\"messageCode\":\"MSG_A\"}"));
        assertNull(CorrelationIdContext.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    void recibir_confirma_o_devuelve_el_mensaje_al_broker() {
        final Consumer<String> consumer = mock(Consumer.class);
        final Message<String> ok = mock(Message.class);
        final Message<String> malo = mock(Message.class);
        when(ok.getValue()).thenReturn("{\"eventType\":\"MessageDeleted\",\"messageCode\":\"MSG_A\"}");
        when(malo.getValue()).thenReturn("{\"eventType\":\"MessageUpdated\",\"messageCode\":\"MSG_B\"}");
        when(puerto.procesarAviso(new AvisoCambioMensaje(null, "MessageDeleted", "MSG_A"))).thenReturn(ResumenSincronizacion.vacio());
        when(puerto.procesarAviso(new AvisoCambioMensaje(null, "MessageUpdated", "MSG_B"))).thenThrow(new IllegalStateException());

        final MessageUcoLabEventListener listener = listener(false);
        listener.recibir(consumer, ok);
        listener.recibir(consumer, malo);

        verify(consumer).acknowledgeAsync(ok);
        verify(consumer).negativeAcknowledge(malo);
        verify(consumer, never()).acknowledgeAsync(malo);
    }

    @Test
    @SuppressWarnings("unchecked")
    void start_se_suscribe_al_topic_de_la_identidad_reconcilia_y_stop_cierra() {
        final ConsumerBuilder<String> builder = mock(ConsumerBuilder.class, invocacion ->
                invocacion.getMethod().getReturnType().isInstance(invocacion.getMock()) ? invocacion.getMock() : null);
        final Consumer<String> consumer = mock(Consumer.class);
        when(client.newConsumer(Schema.STRING)).thenReturn(builder);
        doAnswer(invocacion -> CompletableFuture.completedFuture(consumer)).when(builder).subscribeAsync();
        when(consumer.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));
        when(puerto.reconciliarTodo()).thenReturn(ResumenSincronizacion.vacio());

        final MessageUcoLabEventListener listener = listener(true);
        listener.start();
        listener.start();

        assertTrue(listener.isRunning());
        verify(client, times(1)).newConsumer(Schema.STRING);
        verify(builder).topic(TOPIC);
        verify(builder).subscriptionName("asistencias");
        verify(builder).subscriptionType(SubscriptionType.Failover);
        verify(builder).subscriptionInitialPosition(SubscriptionInitialPosition.Earliest);
        verify(puerto, timeout(2000)).reconciliarTodo();

        listener.stop();
        listener.stop();
        assertFalse(listener.isRunning());
        verify(consumer, times(1)).closeAsync();
    }

    @Test
    @SuppressWarnings("unchecked")
    void sin_pulsar_el_backend_sigue_y_no_reconcilia_si_esta_desactivado() {
        final ConsumerBuilder<String> builder = mock(ConsumerBuilder.class, invocacion ->
                invocacion.getMethod().getReturnType().isInstance(invocacion.getMock()) ? invocacion.getMock() : null);
        when(client.newConsumer(Schema.STRING)).thenReturn(builder);
        doAnswer(invocacion -> CompletableFuture.failedFuture(new IllegalStateException("sin broker"))).when(builder).subscribeAsync();

        final MessageUcoLabEventListener listener = listener(false);
        listener.start();
        listener.stop();

        verify(puerto, never()).reconciliarTodo();
        assertFalse(listener.isRunning());
    }

    /**
     * Regresion del modo contenedores (2026-11-15): el Backend arranco antes de que Pulsar terminara de cargar sus topics,
     * la primera suscripcion vencio y el listener no volvia a intentarlo; los avisos no llegaban hasta reiniciar el Backend.
     */
    @Test
    @SuppressWarnings("unchecked")
    void si_la_primera_suscripcion_falla_se_reintenta_hasta_lograrla() {
        final ConsumerBuilder<String> builder = mock(ConsumerBuilder.class, invocacion ->
                invocacion.getMethod().getReturnType().isInstance(invocacion.getMock()) ? invocacion.getMock() : null);
        final Consumer<String> consumer = mock(Consumer.class);
        when(client.newConsumer(Schema.STRING)).thenReturn(builder);
        when(builder.subscribeAsync())
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker cargando")))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker cargando")))
                .thenReturn(CompletableFuture.completedFuture(consumer));
        when(consumer.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));

        final MessageUcoLabEventListener listener = new MessageUcoLabEventListener(client, puerto, JsonMapper.builder().build(),
                TOPIC, "asistencias", false, Duration.ofMillis(10));
        listener.start();

        verify(builder, timeout(3000).times(3)).subscribeAsync();
        listener.stop();
        verify(consumer, timeout(2000)).closeAsync();
    }

    @Test
    @SuppressWarnings("unchecked")
    void stop_cancela_los_reintentos_pendientes() throws InterruptedException {
        final ConsumerBuilder<String> builder = mock(ConsumerBuilder.class, invocacion ->
                invocacion.getMethod().getReturnType().isInstance(invocacion.getMock()) ? invocacion.getMock() : null);
        when(client.newConsumer(Schema.STRING)).thenReturn(builder);
        doAnswer(invocacion -> CompletableFuture.failedFuture(new IllegalStateException("sin broker"))).when(builder).subscribeAsync();

        final MessageUcoLabEventListener listener = new MessageUcoLabEventListener(client, puerto, JsonMapper.builder().build(),
                TOPIC, "asistencias", false, Duration.ofMillis(50));
        listener.start();
        listener.stop();
        Thread.sleep(300);

        verify(builder, times(1)).subscribeAsync();
    }

    @Test
    void la_espera_entre_reintentos_crece_hasta_un_tope() {
        final MessageUcoLabEventListener listener = new MessageUcoLabEventListener(client, puerto, JsonMapper.builder().build(),
                TOPIC, "asistencias", false, Duration.ofSeconds(15));
        assertEquals(Duration.ofSeconds(15), listener.esperaAntesDelIntento(1));
        assertEquals(Duration.ofSeconds(30), listener.esperaAntesDelIntento(2));
        assertEquals(Duration.ofSeconds(60), listener.esperaAntesDelIntento(3));
        assertEquals(Duration.ofMinutes(5), listener.esperaAntesDelIntento(10));
        assertEquals(Duration.ofMinutes(5), listener.esperaAntesDelIntento(500));
        assertThrows(IllegalArgumentException.class, () -> new MessageUcoLabEventListener(client, puerto,
                JsonMapper.builder().build(), TOPIC, "asistencias", false, Duration.ZERO));
    }

    @Test
    void stop_sin_start_no_hace_nada() {
        final MessageUcoLabEventListener listener = listener(false);
        listener.stop();
        assertFalse(listener.isRunning());
        verifyNoInteractions(client);
    }

    @Test
    void reconciliar_absorbe_fallos_y_limpia_la_correlacion() {
        when(puerto.reconciliarTodo()).thenThrow(new IllegalStateException("caido"));
        listener(true).reconciliar();
        assertNull(CorrelationIdContext.get());
        final ArgumentCaptor<AvisoCambioMensaje> nada = ArgumentCaptor.forClass(AvisoCambioMensaje.class);
        verify(puerto, never()).procesarAviso(nada.capture());
    }

    @Test
    void dependencias_obligatorias() {
        final JsonMapper mapper = JsonMapper.builder().build();
        assertThrows(NullPointerException.class, () -> new MessageUcoLabEventListener(null, puerto, mapper, TOPIC, "s", false));
        assertThrows(NullPointerException.class, () -> new MessageUcoLabEventListener(client, null, mapper, TOPIC, "s", false));
        assertThrows(NullPointerException.class, () -> new MessageUcoLabEventListener(client, puerto, null, TOPIC, "s", false));
        assertThrows(NullPointerException.class, () -> new MessageUcoLabEventListener(client, puerto, mapper, null, "s", false));
        assertThrows(NullPointerException.class, () -> new MessageUcoLabEventListener(client, puerto, mapper, TOPIC, null, false));
    }
}
