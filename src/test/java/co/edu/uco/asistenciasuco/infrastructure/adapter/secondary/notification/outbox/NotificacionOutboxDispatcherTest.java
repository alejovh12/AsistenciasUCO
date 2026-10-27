package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox;

import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.OutboxNotificationProperties;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
                Duration.ofMillis(100),
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
}
