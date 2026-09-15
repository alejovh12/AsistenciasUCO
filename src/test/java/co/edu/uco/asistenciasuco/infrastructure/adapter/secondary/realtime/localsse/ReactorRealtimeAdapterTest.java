package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse;

import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.BaseSubscriber;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactorRealtimeAdapterTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final ReactorRealtimeAdapter adapter = new ReactorRealtimeAdapter(meterRegistry);

    @AfterEach
    void clearCorrelation() {
        CorrelationIdContext.clear();
    }

    @Test
    void un_subscriptor_recibe_el_evento_publicado() {
        final RealtimeEvent event = RealtimeEvent.of(
                "ASISTENCIA_REGISTRADA",
                Map.of("estudiante", "123")
        );

        StepVerifier.create(adapter.events())
                .then(() -> adapter.publish(event))
                .assertNext(received -> {
                    assertEquals(event.eventId(), received.eventId());
                    assertEquals("ASISTENCIA_REGISTRADA", received.type());
                    assertEquals("123", received.payload().get("estudiante"));
                })
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void varios_subscriptores_reciben_el_mismo_evento() throws InterruptedException {
        final RealtimeEvent event = RealtimeEvent.of(
                "SESION_ACTUALIZADA",
                Map.of("sesion", "abc")
        );
        final CountDownLatch bothSubscribed = new CountDownLatch(2);
        final List<UUID> firstReceived = new ArrayList<>();
        final List<UUID> secondReceived = new ArrayList<>();
        final CountDownLatch bothReceived = new CountDownLatch(2);

        final BaseSubscriber<RealtimeEvent> first = subscriber(
                bothSubscribed,
                firstReceived,
                bothReceived
        );
        final BaseSubscriber<RealtimeEvent> second = subscriber(
                bothSubscribed,
                secondReceived,
                bothReceived
        );

        adapter.events().subscribe(first);
        adapter.events().subscribe(second);

        try {
            assertTrue(bothSubscribed.await(2, TimeUnit.SECONDS));
            assertEquals(2, adapter.activeSubscribersCount());

            adapter.publish(event);

            assertTrue(bothReceived.await(2, TimeUnit.SECONDS));
            assertEquals(List.of(event.eventId()), firstReceived);
            assertEquals(List.of(event.eventId()), secondReceived);
        } finally {
            first.cancel();
            second.cancel();
        }
    }

    @Test
    void los_eventos_mantienen_el_orden_para_un_consumidor_disponible() {
        final RealtimeEvent first = RealtimeEvent.of("A", Map.of("orden", 1));
        final RealtimeEvent second = RealtimeEvent.of("B", Map.of("orden", 2));
        final RealtimeEvent third = RealtimeEvent.of("C", Map.of("orden", 3));

        StepVerifier.create(adapter.events())
                .then(() -> {
                    adapter.publish(first);
                    adapter.publish(second);
                    adapter.publish(third);
                })
                .expectNextMatches(event -> event.eventId().equals(first.eventId()))
                .expectNextMatches(event -> event.eventId().equals(second.eventId()))
                .expectNextMatches(event -> event.eventId().equals(third.eventId()))
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void publicar_sin_subscriptores_descarta_sin_lanzar_y_lo_mide() {
        final double publishedBefore = counter("realtime.events.published");
        final double droppedBefore = counter("realtime.events.dropped");

        assertDoesNotThrow(() -> adapter.publish(RealtimeEvent.of("SIN_SUBSCRIPTORES", Map.of())));

        assertEquals(publishedBefore, counter("realtime.events.published"));
        assertEquals(droppedBefore + 1, counter("realtime.events.dropped"));
        assertEquals(0, adapter.activeSubscribersCount());
    }

    @Test
    void evento_publicado_antes_de_conectar_no_se_reproduce_a_un_subscriptor_tardio() {
        adapter.publish(RealtimeEvent.of("ANTES", Map.of()));

        final RealtimeEvent after = RealtimeEvent.of("DESPUES", Map.of());

        StepVerifier.create(adapter.events())
                .then(() -> adapter.publish(after))
                .assertNext(received -> assertEquals(after.eventId(), received.eventId()))
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void desconexion_de_un_subscriptor_no_impide_conexiones_futuras() {
        final reactor.core.Disposable firstSubscription = adapter.events().subscribe();
        assertEquals(1, adapter.activeSubscribersCount());

        firstSubscription.dispose();
        awaitSubscriberCount(0);

        final RealtimeEvent event = RealtimeEvent.of("NUEVA_CONEXION", Map.of());
        StepVerifier.create(adapter.events())
                .then(() -> adapter.publish(event))
                .assertNext(received -> assertEquals(event.eventId(), received.eventId()))
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void publish_enriquece_el_evento_con_el_correlationId_del_contexto() {
        final UUID correlationId = UUID.randomUUID();
        CorrelationIdContext.set(correlationId);
        final RealtimeEvent event = RealtimeEvent.of("CON_CORRELACION", Map.of());

        StepVerifier.create(adapter.events())
                .then(() -> adapter.publish(event))
                .assertNext(received -> assertEquals(
                        correlationId.toString(),
                        received.correlationId()
                ))
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void publish_con_evento_nulo_no_lanza_y_se_contabiliza_como_descartado() {
        final double before = counter("realtime.events.dropped");

        assertDoesNotThrow(() -> adapter.publish(null));

        assertEquals(before + 1, counter("realtime.events.dropped"));
    }

    @Test
    void publish_exitoso_incrementa_la_metrica_de_eventos_publicados() {
        final double before = counter("realtime.events.published");
        final RealtimeEvent event = RealtimeEvent.of("METRICA", Map.of());

        StepVerifier.create(adapter.events())
                .then(() -> adapter.publish(event))
                .expectNextCount(1)
                .thenCancel()
                .verify(Duration.ofSeconds(2));

        assertEquals(before + 1, counter("realtime.events.published"));
    }

    @Test
    void gauge_de_subscriptores_activos_refleja_el_conteo_en_tiempo_real() {
        assertEquals(0, meterRegistry.get("realtime.subscribers.active").gauge().value());

        final reactor.core.Disposable subscription = adapter.events().subscribe();
        try {
            assertEquals(1, meterRegistry.get("realtime.subscribers.active").gauge().value());
        } finally {
            subscription.dispose();
        }
    }

    @Test
    void subscriptor_sin_demanda_no_bloquea_a_otro_disponible() throws InterruptedException {
        final AtomicInteger slowReceived = new AtomicInteger();
        final CountDownLatch slowSubscribed = new CountDownLatch(1);

        final BaseSubscriber<RealtimeEvent> slowSubscriber = new BaseSubscriber<>() {
            @Override
            protected void hookOnSubscribe(final org.reactivestreams.Subscription subscription) {
                slowSubscribed.countDown();
            }

            @Override
            protected void hookOnNext(final RealtimeEvent value) {
                slowReceived.incrementAndGet();
            }
        };

        adapter.events().subscribe(slowSubscriber);
        assertTrue(slowSubscribed.await(2, TimeUnit.SECONDS));

        final RealtimeEvent event = RealtimeEvent.of("BEST_EFFORT", Map.of());

        try {
            StepVerifier.create(adapter.events())
                    .then(() -> adapter.publish(event))
                    .assertNext(received -> assertEquals(event.eventId(), received.eventId()))
                    .thenCancel()
                    .verify(Duration.ofSeconds(2));

            assertEquals(0, slowReceived.get());
        } finally {
            slowSubscriber.cancel();
        }
    }

    private double counter(final String name) {
        return meterRegistry.get(name).counter().count();
    }

    private static BaseSubscriber<RealtimeEvent> subscriber(
            final CountDownLatch subscribed,
            final List<UUID> received,
            final CountDownLatch receivedLatch
    ) {
        return new BaseSubscriber<>() {
            @Override
            protected void hookOnSubscribe(final org.reactivestreams.Subscription subscription) {
                request(Long.MAX_VALUE);
                subscribed.countDown();
            }

            @Override
            protected void hookOnNext(final RealtimeEvent value) {
                received.add(value.eventId());
                receivedLatch.countDown();
            }
        };
    }

    private void awaitSubscriberCount(final int expected) {
        final long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (adapter.activeSubscribersCount() != expected && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertEquals(expected, adapter.activeSubscribersCount());
    }
}
