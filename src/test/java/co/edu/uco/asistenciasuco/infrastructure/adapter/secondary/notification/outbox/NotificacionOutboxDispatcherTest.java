package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox;

import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.OutboxNotificationProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.OutboxNotificationProperties.Sender;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacionOutboxDispatcherTest {

    private NotificacionOutboxRepository repository;
    private NotificacionChannelSender sender;
    private MeterRegistry meterRegistry;
    private OutboxNotificationProperties properties;
    private NotificacionOutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        repository = mock(NotificacionOutboxRepository.class);
        sender = mock(NotificacionChannelSender.class);
        meterRegistry = new SimpleMeterRegistry();
        properties = new OutboxNotificationProperties(
                OutboxNotificationProperties.Sender.LOG,
                true,
                50,
                Duration.ofSeconds(60),
                Duration.ofSeconds(1),
                5,
                Duration.ofSeconds(30),
                Duration.ofMinutes(15),
                30,
                Duration.ofHours(6),
                "test-instance-1"
        );
    }

    @AfterEach
    void tearDown() {
        if (dispatcher != null && dispatcher.isRunning()) {
            dispatcher.stop();
        }
    }

    @Test
    @DisplayName("BE-D01: envio exitoso llama a marcarEntregada")
    void BE_D01_exito_marca_entregada() throws Exception {
        final UUID id = UUID.randomUUID();
        final NotificacionReservada reservada = new NotificacionReservada(
                id, "TIPO", "Asunto", "Mensaje", "[]", "{}", 1, 5, Instant.now(), null, null
        );

        when(repository.reservar(anyInt(), any(Duration.class), any()))
                .thenReturn(List.of(reservada))
                .thenReturn(List.of());

        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        dispatcher.ejecutarCiclo();

        verify(sender).enviar(any(NotificacionPersistida.class));
        verify(repository).marcarEntregada(eq(id), eq("test-instance-1"));

        final double delivered = meterRegistry.get("notification.dispatch.total")
                .tag("outcome", "delivered")
                .counter().count();
        assertEquals(1.0, delivered);
    }

    @Test
    @DisplayName("BE-D02: sender lanza NotificacionNoEntregableException registra fallo no reintentable")
    void BE_D02_fallo_no_reintentable() throws Exception {
        final UUID id = UUID.randomUUID();
        final NotificacionReservada reservada = new NotificacionReservada(
                id, "TIPO", "Asunto", "Mensaje", "[]", "{}", 1, 5, Instant.now(), null, null
        );

        when(repository.reservar(anyInt(), any(Duration.class), any()))
                .thenReturn(List.of(reservada))
                .thenReturn(List.of());
        doThrow(new NotificacionNoEntregableException("Correo invalido")).when(sender).enviar(any());

        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        dispatcher.ejecutarCiclo();

        verify(repository).registrarFallo(
                eq(id), eq("test-instance-1"), any(), any(), eq(false), anyInt(), any(), any()
        );

        final double dead = meterRegistry.get("notification.dispatch.total")
                .tag("outcome", "dead")
                .counter().count();
        assertEquals(1.0, dead);
    }

    @Test
    @DisplayName("BE-D03: sender lanza RuntimeException registra fallo reintentable")
    void BE_D03_fallo_reintentable() throws Exception {
        final UUID id = UUID.randomUUID();
        final NotificacionReservada reservada = new NotificacionReservada(
                id, "TIPO", "Asunto", "Mensaje", "[]", "{}", 1, 5, Instant.now(), null, null
        );

        when(repository.reservar(anyInt(), any(Duration.class), any()))
                .thenReturn(List.of(reservada))
                .thenReturn(List.of());
        doThrow(new RuntimeException("Network error")).when(sender).enviar(any());

        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        dispatcher.ejecutarCiclo();

        verify(repository).registrarFallo(
                eq(id), eq("test-instance-1"), any(), any(), eq(true), eq(5), eq(Duration.ofSeconds(30)), eq(Duration.ofMinutes(15))
        );

        final double retry = meterRegistry.get("notification.dispatch.total")
                .tag("outcome", "retry")
                .counter().count();
        assertEquals(1.0, retry);
    }

    @Test
    @DisplayName("BE-D04: marcarEntregada lanza reserva perdida e incrementa lost_lease")
    void BE_D04_reserva_perdida_al_marcar_entregada() throws Exception {
        final UUID id = UUID.randomUUID();
        final NotificacionReservada reservada = new NotificacionReservada(
                id, "TIPO", "Asunto", "Mensaje", "[]", "{}", 1, 5, Instant.now(), null, null
        );

        when(repository.reservar(anyInt(), any(Duration.class), any()))
                .thenReturn(List.of(reservada))
                .thenReturn(List.of());
        doThrow(new RuntimeException("Reserva perdida")).when(repository).marcarEntregada(any(), any());

        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        dispatcher.ejecutarCiclo();

        final double lostLease = meterRegistry.get("notification.dispatch.total")
                .tag("outcome", "lost_lease")
                .counter().count();
        assertEquals(1.0, lostLease);
    }

    @Test
    @DisplayName("BE-D05: reservar lanza y el despachador sobrevive")
    void BE_D05_reservar_lanza_y_continua() {
        when(repository.reservar(anyInt(), any(Duration.class), any()))
                .thenThrow(new RuntimeException("DB down"));

        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        dispatcher.ejecutarCiclo();

        verify(repository).reservar(anyInt(), any(Duration.class), any());
    }

    @Test
    @DisplayName("BE-D08: dispatcherEnabled=false no arranca hilo")
    void BE_D08_deshabilitado_no_inicia() {
        final OutboxNotificationProperties deshabilitado = new OutboxNotificationProperties(
                OutboxNotificationProperties.Sender.LOG,
                false,
                50,
                Duration.ofSeconds(60),
                Duration.ofSeconds(30),
                5,
                Duration.ofSeconds(30),
                Duration.ofMinutes(15),
                30,
                Duration.ofHours(6),
                "test-instance"
        );

        dispatcher = new NotificacionOutboxDispatcher(repository, sender, deshabilitado, meterRegistry, Clock.systemUTC());
        dispatcher.start();

        assertFalse(dispatcher.isRunning());
    }

    @Test
    @DisplayName("BE-D09: depuracion al arrancar")
    void BE_D09_depuracion_al_arrancar() {
        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        dispatcher.ejecutarDepuracion();

        verify(repository).depurar(30, 500);
    }

    @Test
    @DisplayName("BE-D12: instance-id vacio genera hostname estable")
    void BE_D12_instance_id_vacio_genera_identificador() {
        final OutboxNotificationProperties sinId = new OutboxNotificationProperties(
                OutboxNotificationProperties.Sender.LOG,
                true,
                50,
                Duration.ofSeconds(60),
                Duration.ofSeconds(30),
                5,
                Duration.ofSeconds(30),
                Duration.ofMinutes(15),
                30,
                Duration.ofHours(6),
                ""
        );

        final NotificacionOutboxDispatcher disp1 = new NotificacionOutboxDispatcher(repository, sender, sinId, meterRegistry, Clock.systemUTC());
        assertNotNull(disp1.instanceId());
        assertFalse(disp1.instanceId().isBlank());
        assertEquals(disp1.instanceId(), disp1.instanceId());
    }

    @Test
    @DisplayName("BE-D13: getPhase y signal funcionan correctamente")
    void BE_D13_get_phase_y_signal() {
        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        assertTrue(dispatcher.getPhase() > 0);
        assertDoesNotThrow(() -> dispatcher.signal());
    }

    @Test
    @DisplayName("BE-D14: NotificacionNoEntregableException con causa")
    void BE_D14_excepcion_con_causa() {
        final RuntimeException causa = new RuntimeException("Error raiz");
        final NotificacionNoEntregableException ex = new NotificacionNoEntregableException("Mensaje", causa);
        assertEquals("Mensaje", ex.getMessage());
        assertEquals(causa, ex.getCause());
    }

    @Test
    @DisplayName("BE-D15: hilo en segundo plano arranca y se detiene")
    void BE_D15_hilo_arranca_y_se_detiene() throws InterruptedException {
        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        dispatcher.start();
        assertTrue(dispatcher.isRunning());
        Thread.sleep(50);
        dispatcher.stop();
        assertFalse(dispatcher.isRunning());
    }

    @Test
    @DisplayName("BE-D16: depuracion tolera excepcion en repositorio")
    void BE_D16_depuracion_tolera_excepcion() {
        when(repository.depurar(anyInt(), anyInt())).thenThrow(new RuntimeException("Fallo DB depurar"));
        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        assertDoesNotThrow(() -> dispatcher.ejecutarDepuracion());
    }

    @Test
    @DisplayName("BE-D17: lote vacio no procesa notificaciones")
    void BE_D17_lote_vacio_no_procesa() {
        when(repository.reservar(anyInt(), any(Duration.class), any())).thenReturn(List.of());
        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        dispatcher.ejecutarCiclo();
        verify(repository).reservar(anyInt(), any(Duration.class), any());
    }

    @Test
    @DisplayName("BE-D18: deserializacion maneja json nulo, vacio o invalido")
    void BE_D18_deserializacion_json_casos_borde() throws Exception {
        final UUID id = UUID.randomUUID();
        final NotificacionReservada reservada = new NotificacionReservada(
                id, "TIPO", "Asunto", "Mensaje", "{no es array}", "{no es json valido}", 1, 5, Instant.now(), null, null
        );

        when(repository.reservar(anyInt(), any(Duration.class), any()))
                .thenReturn(List.of(reservada))
                .thenReturn(List.of());

        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        dispatcher.ejecutarCiclo();

        verify(sender).enviar(any(NotificacionPersistida.class));
        verify(repository).marcarEntregada(eq(id), any());
    }

    @Test
    @DisplayName("BE-D19: depuracion con eliminadas mayor a cero incrementa metricas")
    void BE_D19_depuracion_con_filas_eliminadas() {
        when(repository.depurar(anyInt(), anyInt())).thenReturn(25);
        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        dispatcher.ejecutarDepuracion();

        final double purged = meterRegistry.get("notification.outbox.purged.total").counter().count();
        assertEquals(25.0, purged);
    }

    @Test
    @DisplayName("BE-D20: fallo al marcar entregada incrementa lost-lease")
    void BE_D20_fallo_marcar_entregada_lost_lease() {
        final UUID id = UUID.randomUUID();
        final NotificacionReservada reservada = new NotificacionReservada(
                id, "TIPO", "Asunto", "Mensaje", "[]", "{}", 1, 5, Instant.now(), null, null
        );
        when(repository.reservar(anyInt(), any(Duration.class), any()))
                .thenReturn(List.of(reservada))
                .thenReturn(List.of());
        doThrow(new RuntimeException("Lease expirado")).when(repository).marcarEntregada(eq(id), any());

        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        dispatcher.ejecutarCiclo();

        final double lostLease = meterRegistry.get("notification.dispatch.total")
                .tag("outcome", "lost_lease")
                .counter().count();
        assertEquals(1.0, lostLease);
    }

    @Test
    @DisplayName("BE-D21: fallo al registrar fallo definitivo incrementa lost-lease")
    void BE_D21_fallo_registrar_fallo_definitivo_lost_lease() throws Exception {
        final UUID id = UUID.randomUUID();
        final NotificacionReservada reservada = new NotificacionReservada(
                id, "TIPO", "Asunto", "Mensaje", "[]", "{}", 1, 5, Instant.now(), null, null
        );
        when(repository.reservar(anyInt(), any(Duration.class), any()))
                .thenReturn(List.of(reservada))
                .thenReturn(List.of());
        doThrow(new NotificacionNoEntregableException("Correo invalido")).when(sender).enviar(any());
        doThrow(new RuntimeException("DB down")).when(repository).registrarFallo(eq(id), any(), eq("FALLO_DEFINITIVO"), any(), eq(false), anyInt(), any(), any());

        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        dispatcher.ejecutarCiclo();

        final double lostLease = meterRegistry.get("notification.dispatch.total")
                .tag("outcome", "lost_lease")
                .counter().count();
        assertEquals(1.0, lostLease);
    }

    @Test
    @DisplayName("BE-D22: fallo al registrar fallo reintentable incrementa lost-lease")
    void BE_D22_fallo_registrar_fallo_reintentable_lost_lease() throws Exception {
        final UUID id = UUID.randomUUID();
        final NotificacionReservada reservada = new NotificacionReservada(
                id, "TIPO", "Asunto", "Mensaje", "[]", "{}", 1, 5, Instant.now(), null, null
        );
        when(repository.reservar(anyInt(), any(Duration.class), any()))
                .thenReturn(List.of(reservada))
                .thenReturn(List.of());
        doThrow(new RuntimeException("Timeout temporal")).when(sender).enviar(any());
        doThrow(new RuntimeException("DB down")).when(repository).registrarFallo(eq(id), any(), eq("FALLO_REINTENTABLE"), any(), eq(true), anyInt(), any(), any());

        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        dispatcher.ejecutarCiclo();

        final double lostLease = meterRegistry.get("notification.dispatch.total")
                .tag("outcome", "lost_lease")
                .counter().count();
        assertEquals(1.0, lostLease);
    }

    @Test
    @DisplayName("BE-D23: start con dispatcherEnabled=false no arranca")
    void BE_D23_start_deshabilitado() {
        final OutboxNotificationProperties propsDisabled = new OutboxNotificationProperties(
                Sender.LOG, false, 50, Duration.ofSeconds(60), Duration.ofSeconds(30), 5,
                Duration.ofSeconds(30), Duration.ofMinutes(15), 30, Duration.ofHours(6), "inst-disabled"
        );
        dispatcher = new NotificacionOutboxDispatcher(repository, sender, propsDisabled, meterRegistry, Clock.systemUTC());
        dispatcher.start();
        assertFalse(dispatcher.isRunning());
    }

    @Test
    @DisplayName("BE-D24: stop sin start no falla")
    void BE_D24_stop_sin_start() {
        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        assertDoesNotThrow(() -> dispatcher.stop());
    }

    @Test
    @DisplayName("BE-D25: depuracion con cero eliminadas no incrementa purgedCounter")
    void BE_D25_depuracion_cero_filas() {
        when(repository.depurar(anyInt(), anyInt())).thenReturn(0);
        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        dispatcher.ejecutarDepuracion();

        final double purged = meterRegistry.get("notification.outbox.purged.total").counter().count();
        assertEquals(0.0, purged);
    }

    @Test
    @DisplayName("BE-D26: depuracion periodica se ejecuta cuando transcurre retentionInterval")
    void BE_D26_depuracion_periodica_se_ejecuta() {
        final Instant t0 = Instant.parse("2026-10-27T10:00:00Z");
        final Clock mockClock = mock(Clock.class);
        when(mockClock.instant())
                .thenReturn(t0)
                .thenReturn(t0.plus(Duration.ofHours(7)));
        when(mockClock.millis()).thenReturn(t0.toEpochMilli());

        when(repository.depurar(anyInt(), anyInt())).thenReturn(10);

        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, mockClock);
        dispatcher.verificarDepuracionPeriodica();

        verify(repository, atLeastOnce()).depurar(anyInt(), anyInt());
    }

    @Test
    @DisplayName("BE-D27: start llamado dos veces no recrea hilo")
    void BE_D27_start_llamado_dos_veces() {
        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        dispatcher.start();
        dispatcher.start();
        assertTrue(dispatcher.isRunning());
        dispatcher.stop();
    }

    @Test
    @DisplayName("BE-D28: stop llamado dos veces es idempotente")
    void BE_D28_stop_llamado_dos_veces() {
        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        dispatcher.start();
        dispatcher.stop();
        dispatcher.stop();
        assertFalse(dispatcher.isRunning());
    }

    @Test
    @DisplayName("BE-D29: lote nulo no lanza excepcion ni procesa")
    void BE_D29_lote_nulo_no_falla() {
        when(repository.reservar(anyInt(), any(Duration.class), any())).thenReturn(null);
        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        assertDoesNotThrow(() -> dispatcher.ejecutarCiclo());
    }

    @Test
    @DisplayName("BE-D30: deserializacion de destinatarios y datos con nulo y blanco")
    void BE_D30_deserializacion_nulo_y_blanco() throws Exception {
        final UUID id1 = UUID.randomUUID();
        final NotificacionReservada res1 = new NotificacionReservada(
                id1, "TIPO", "Asunto", "Mensaje", null, null, 1, 5, Instant.now(), null, null
        );
        final UUID id2 = UUID.randomUUID();
        final NotificacionReservada res2 = new NotificacionReservada(
                id2, "TIPO", "Asunto", "Mensaje", "   ", "   ", 1, 5, Instant.now(), null, null
        );

        when(repository.reservar(anyInt(), any(Duration.class), any()))
                .thenReturn(List.of(res1, res2))
                .thenReturn(List.of());

        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        dispatcher.ejecutarCiclo();

        verify(sender, times(2)).enviar(any(NotificacionPersistida.class));
    }

    @Test
    @DisplayName("BE-D31: dispatcherEnabled=false no arranca workerThread")
    void BE_D31_dispatcher_deshabilitado_no_inicia() {
        final OutboxNotificationProperties propsDisabled = new OutboxNotificationProperties(
                Sender.LOG, false, 50, Duration.ofSeconds(60), Duration.ofSeconds(1),
                5, Duration.ofSeconds(30), Duration.ofMinutes(15), 30, Duration.ofHours(6), "inst-disabled"
        );
        final NotificacionOutboxDispatcher dis = new NotificacionOutboxDispatcher(repository, sender, propsDisabled, meterRegistry, Clock.systemUTC());
        dis.start();
        assertFalse(dis.isRunning());
    }

    @Test
    @DisplayName("BE-D32: fallo al marcar entregada incrementa lostLeaseCounter")
    void BE_D32_fallo_marcar_entregada_incrementa_lost_lease() throws Exception {
        final UUID id = UUID.randomUUID();
        final NotificacionReservada res = new NotificacionReservada(
                id, "TIPO", "Asunto", "Mensaje", "[]", "{}", 1, 5, Instant.now(), null, null
        );
        when(repository.reservar(anyInt(), any(Duration.class), any())).thenReturn(List.of(res)).thenReturn(List.of());
        doThrow(new RuntimeException("DB down")).when(repository).marcarEntregada(eq(id), any());

        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        dispatcher.ejecutarCiclo();

        final double lostLease = meterRegistry.find("notification.dispatch.total")
                .tag("outcome", "lost_lease").counter().count();
        assertEquals(1.0, lostLease);
    }

    @Test
    @DisplayName("BE-D33: fallo al registrar fallo no entregable incrementa lostLeaseCounter")
    void BE_D33_fallo_registrar_fallo_definitivo_incrementa_lost_lease() throws Exception {
        final UUID id = UUID.randomUUID();
        final NotificacionReservada res = new NotificacionReservada(
                id, "TIPO", "Asunto", "Mensaje", "[]", "{}", 1, 5, Instant.now(), null, null
        );
        when(repository.reservar(anyInt(), any(Duration.class), any())).thenReturn(List.of(res)).thenReturn(List.of());
        doThrow(new NotificacionNoEntregableException("Buzon inexistente")).when(sender).enviar(any());
        doThrow(new RuntimeException("DB down")).when(repository).registrarFallo(any(), any(), any(), any(), anyBoolean(), anyInt(), any(), any());

        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        dispatcher.ejecutarCiclo();

        final double lostLease = meterRegistry.find("notification.dispatch.total")
                .tag("outcome", "lost_lease").counter().count();
        assertEquals(1.0, lostLease);
    }

    @Test
    @DisplayName("BE-D34: fallo transitorio con intentos >= maxAttempts incrementa deadCounter")
    void BE_D34_fallo_transitorio_max_intentos_incrementa_dead() throws Exception {
        final UUID id = UUID.randomUUID();
        // intentos = 5, igual a properties.maxAttempts (5)
        final NotificacionReservada res = new NotificacionReservada(
                id, "TIPO", "Asunto", "Mensaje", "[]", "{}", 5, 5, Instant.now(), null, null
        );
        when(repository.reservar(anyInt(), any(Duration.class), any())).thenReturn(List.of(res)).thenReturn(List.of());
        doThrow(new RuntimeException("Network timeout")).when(sender).enviar(any());

        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        dispatcher.ejecutarCiclo();

        final double dead = meterRegistry.find("notification.dispatch.total")
                .tag("outcome", "dead").counter().count();
        assertEquals(1.0, dead);
    }

    @Test
    @DisplayName("BE-D35: fallo al registrar fallo reintentable incrementa lostLeaseCounter")
    void BE_D35_fallo_registrar_fallo_reintentable_incrementa_lost_lease() throws Exception {
        final UUID id = UUID.randomUUID();
        final NotificacionReservada res = new NotificacionReservada(
                id, "TIPO", "Asunto", "Mensaje", "[]", "{}", 1, 5, Instant.now(), null, null
        );
        when(repository.reservar(anyInt(), any(Duration.class), any())).thenReturn(List.of(res)).thenReturn(List.of());
        doThrow(new RuntimeException("Network timeout")).when(sender).enviar(any());
        doThrow(new RuntimeException("DB down")).when(repository).registrarFallo(any(), any(), any(), any(), anyBoolean(), anyInt(), any(), any());

        dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
        dispatcher.ejecutarCiclo();

        final double lostLease = meterRegistry.find("notification.dispatch.total")
                .tag("outcome", "lost_lease").counter().count();
        assertEquals(1.0, lostLease);
    }

    @Test
    @DisplayName("BE-D36: ejecutarCiclo y ejecutarDepuracion con correlationId preexistente")
    void BE_D36_ciclo_y_depuracion_con_correlation_preexistente() {
        co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext.set(UUID.randomUUID().toString());
        try {
            when(repository.reservar(anyInt(), any(Duration.class), any())).thenReturn(List.of());
            dispatcher = new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
            assertDoesNotThrow(() -> dispatcher.ejecutarCiclo());
            assertDoesNotThrow(() -> dispatcher.ejecutarDepuracion());
        } finally {
            co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext.clear();
        }
    }
}
