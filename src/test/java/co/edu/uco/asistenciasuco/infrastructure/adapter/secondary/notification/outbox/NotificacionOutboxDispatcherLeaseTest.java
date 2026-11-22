package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox;

import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.OutboxNotificationProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reserva con lease: el despachador no debe enviar items cuando la reserva del lote ya vencio
 * (o esta por vencer), porque marcarEntregada fallaria con NTF_003 y el item se reenviaria.
 */
class NotificacionOutboxDispatcherLeaseTest {

    private static final Duration LEASE = Duration.ofSeconds(60);
    private static final Duration ENVIO = Duration.ofSeconds(3);

    /** Reloj falso mutable. */
    private static final class RelojFalso extends Clock {
        private final AtomicReference<Instant> ahora;

        RelojFalso(final Instant inicio) {
            this.ahora = new AtomicReference<>(inicio);
        }

        void avanzar(final Duration d) {
            ahora.updateAndGet(i -> i.plus(d));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora.get();
        }
    }

    private NotificacionOutboxRepository repository;
    private NotificacionChannelSender sender;
    private MeterRegistry meterRegistry;
    private RelojFalso reloj;
    private Instant inicio;
    private OutboxNotificationProperties properties;

    @BeforeEach
    void setUp() {
        repository = mock(NotificacionOutboxRepository.class);
        sender = mock(NotificacionChannelSender.class);
        when(sender.disponible()).thenReturn(true);
        meterRegistry = new SimpleMeterRegistry();
        inicio = Instant.parse("2026-11-22T10:00:00Z");
        reloj = new RelojFalso(inicio);
        properties = new OutboxNotificationProperties(
                OutboxNotificationProperties.Sender.SMTP, true, 50, LEASE, Duration.ofSeconds(1), 5,
                Duration.ofSeconds(30), Duration.ofMinutes(15), 30, Duration.ofHours(6), "test-instance-1");
    }

    private List<NotificacionReservada> lote(final int n) {
        return IntStream.range(0, n)
                .mapToObj(i -> new NotificacionReservada(UUID.randomUUID(), "TIPO", "Asunto", "Mensaje",
                        "[]", "{}", 1, inicio, null, null, null))
                .toList();
    }

    private double contador(final String outcome) {
        return meterRegistry.get("notification.dispatch.total").tag("outcome", outcome).counter().count();
    }

    @Test
    @DisplayName("BE-L01: lote lento no envia items cuando la reserva ya vencio ni los marca fallidos")
    void BE_L01_no_envia_despues_del_vencimiento_del_lease() throws Exception {
        when(repository.reservar(anyInt(), any(Duration.class), any())).thenReturn(lote(46));

        final List<Instant> inicios = new ArrayList<>();
        final Instant vence = inicio.plus(LEASE);
        doAnswer(inv -> {
            inicios.add(reloj.instant());
            reloj.avanzar(ENVIO);
            return null;
        }).when(sender).enviar(any(NotificacionPersistida.class));
        // Simula NTF_003 del SP: la reserva ya vencio.
        doAnswer(inv -> {
            if (!reloj.instant().isBefore(vence)) {
                throw new IllegalStateException("NTF_003 reserva vencida");
            }
            return null;
        }).when(repository).marcarEntregada(any(UUID.class), anyString());

        final NotificacionOutboxDispatcher dispatcher =
                new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, reloj);
        dispatcher.ejecutarCiclo();

        assertTrue(inicios.size() < 46, "no debe enviar todo el lote");
        assertTrue(inicios.stream().allMatch(i -> i.plus(ENVIO).isBefore(vence)),
                "ningun envio debe terminar despues del vencimiento del lease");
        assertEquals(0.0, contador("lost_lease"));
        assertEquals(inicios.size(), (int) contador("delivered"));
        assertEquals(46 - inicios.size(), (int) contador("skipped_lease"));
        // Sin incremento de intentos ni marca de fallo.
        verify(repository, never()).registrarFallo(any(), any(), any(), any(), anyBoolean(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("BE-L02: lote rapido dentro del lease se envia completo")
    void BE_L02_lote_rapido_no_se_afecta() throws Exception {
        when(repository.reservar(anyInt(), any(Duration.class), any())).thenReturn(lote(10));
        doAnswer(inv -> {
            reloj.avanzar(Duration.ofMillis(50));
            return null;
        }).when(sender).enviar(any(NotificacionPersistida.class));

        final NotificacionOutboxDispatcher dispatcher =
                new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, reloj);
        dispatcher.ejecutarCiclo();

        verify(sender, times(10)).enviar(any(NotificacionPersistida.class));
        verify(repository, times(10)).marcarEntregada(any(UUID.class), eq("test-instance-1"));
        assertEquals(0.0, contador("skipped_lease"));
    }

    @Test
    @DisplayName("BE-L03: el margen se adapta al tiempo de envio observado")
    void BE_L03_margen_adaptativo() throws Exception {
        // Envio de 20s: margen = 2 x 20 = 40s. Quedan 60s -> envia; quedan 40s (>= 40) -> envia;
        // quedan 20s -> omite los 3 restantes.
        when(repository.reservar(anyInt(), any(Duration.class), any())).thenReturn(lote(5));
        doAnswer(inv -> {
            reloj.avanzar(Duration.ofSeconds(20));
            return null;
        }).when(sender).enviar(any(NotificacionPersistida.class));

        final NotificacionOutboxDispatcher dispatcher =
                new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, reloj);
        dispatcher.ejecutarCiclo();

        verify(sender, times(2)).enviar(any(NotificacionPersistida.class));
        assertEquals(3, (int) contador("skipped_lease"));
    }
}
