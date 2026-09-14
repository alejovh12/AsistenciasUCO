package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime;

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
        final RealtimeEvent event = RealtimeEvent.of("ASISTENCIA_REGISTRADA", Map.of("estudiante", "123"));

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
        final RealtimeEvent event = RealtimeEvent.of("SESION_ACTUALIZADA", Map.of("sesion", "abc"));
        final CountDownLatch bothSubscribed = new CountDownLatch(2);
        final List<UUID> receivedBySubscriberOne = new ArrayList<>();
        final List<UUID> receivedBySubscriberTwo = new ArrayList<>();
        final CountDownLatch bothReceived = new CountDownLatch(2);

        adapter.events().subscribe(new BaseSubscriber<RealtimeEvent>() {
            @Override
            protected void hookOnSubscribe(final org.reactivestreams.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
                bothSubscribed.countDown();
            }

            @Override
            protected void hookOnNext(final RealtimeEvent value) {
                receivedBySubscriberOne.add(value.eventId());
                bothReceived.countDown();
            }
        });
        adapter.events().subscribe(new BaseSubscriber<RealtimeEvent>() {
            @Override
            protected void hookOnSubscribe(final org.reactivestreams.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
                bothSubscribed.countDown();
            }

            @Override
            protected void hookOnNext(final RealtimeEvent value) {
                receivedBySubscriberTwo.add(value.eventId());
                bothReceived.countDown();
            }
        });

        assertTrue(bothSubscribed.await(2, TimeUnit.SECONDS));
        assertEquals(2, adapter.activeSubscribersCount());

        adapter.publish(event);

        assertTrue(bothReceived.await(2, TimeUnit.SECONDS));
        assertEquals(List.of(event.eventId()), receivedBySubscriberOne);
        assertEquals(List.of(event.eventId()), receivedBySubscriberTwo);
    }

    @Test
    void los_eventos_mantienen_el_orden_de_publicacion() {
        final RealtimeEvent first = RealtimeEvent.of("A", Map.of("orden", 1));
        final RealtimeEvent second = RealtimeEvent.of("B", Map.of("orden", 2));
        final RealtimeEvent third = RealtimeEvent.of("C", Map.of("orden", 3));

        StepVerifier.create(adapter.events())
                .then(() -> {
                    adapter.publish(first);
                    adapter.publish(second);
                    adapter.publish(third);
                })
                .expectNextMatches(e -> e.eventId().equals(first.eventId()))
                .expectNextMatches(e -> e.eventId().equals(second.eventId()))
                .expectNextMatches(e -> e.eventId().equals(third.eventId()))
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void publicar_sin_subscriptores_no_lanza_excepcion() {
        final RealtimeEvent event = RealtimeEvent.of("SIN_SUBSCRIPTORES", Map.of());

        assertDoesNotThrow(() -> adapter.publish(event));
        assertEquals(0, adapter.activeSubscribersCount());
    }

    @Test
    void desconexion_de_un_subscriptor_no_rompe_el_hub_para_otros() {
        final RealtimeEvent afterCancel = RealtimeEvent.of("DESPUES_DE_CANCELAR", Map.of());

        final reactor.core.Disposable disposableSubscriber = adapter.events().subscribe();
        assertEquals(1, adapter.activeSubscribersCount());
        disposableSubscriber.dispose();
        awaitSubscriberCount(0);

        StepVerifier.create(adapter.events())
                .then(() -> adapter.publish(afterCancel))
                .assertNext(received -> assertEquals(afterCancel.eventId(), received.eventId()))
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
                .assertNext(received -> assertEquals(correlationId.toString(), received.correlationId()))
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void publish_con_evento_nulo_no_lanza_excepcion() {
        assertDoesNotThrow(() -> adapter.publish(null));
    }

    @Test
    void publish_exitoso_incrementa_la_metrica_de_eventos_publicados() {
        final double before = meterRegistry.get("realtime_events_published_total").counter().count();

        adapter.publish(RealtimeEvent.of("METRICA", Map.of()));

        final double after = meterRegistry.get("realtime_events_published_total").counter().count();
        assertEquals(before + 1, after);
    }

    @Test
    void gauge_de_subscriptores_activos_refleja_el_conteo_en_tiempo_real() {
        assertEquals(0, meterRegistry.get("realtime_subscribers_active").gauge().value());

        final reactor.core.Disposable subscription = adapter.events().subscribe();
        try {
            assertEquals(1, meterRegistry.get("realtime_subscribers_active").gauge().value());
        } finally {
            subscription.dispose();
        }
    }

    @Test
    void overflow_del_buffer_se_maneja_sin_lanzar_excepcion_y_se_puede_observar_en_metricas() throws InterruptedException {
        final AtomicInteger requested = new AtomicInteger(0);
        final CountDownLatch subscribed = new CountDownLatch(1);
        // Subscriptor que nunca solicita elementos: fuerza que el buffer acotado se llene.
        adapter.events().subscribe(new BaseSubscriber<RealtimeEvent>() {
            @Override
            protected void hookOnSubscribe(final org.reactivestreams.Subscription subscription) {
                subscribed.countDown();
            }
        });
        assertTrue(subscribed.await(2, TimeUnit.SECONDS));

        for (int i = 0; i < ReactorRealtimeAdapter.BUFFER_SIZE + 10; i++) {
            final int index = i;
            assertDoesNotThrow(() -> adapter.publish(RealtimeEvent.of("OVERFLOW", Map.of("i", index))));
        }

        final double dropped = meterRegistry.get("realtime_events_dropped_total").counter().count();
        assertTrue(dropped > 0, "Se esperaba al menos un evento descartado por overflow del buffer acotado.");
    }

    private void awaitSubscriberCount(final int expected) {
        final long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (adapter.activeSubscribersCount() != expected && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertEquals(expected, adapter.activeSubscribersCount());
    }
}
