package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopeEnviosTest {

    @Test
    @DisplayName("BE-K01: hasta rate-per-minute envios por minuto; el siguiente deja disponible()=false hasta la ventana siguiente")
    void BE_K01_tope_por_minuto_y_recuperacion_en_ventana_siguiente() {
        final MutableClock clock = new MutableClock(Instant.parse("2026-10-30T10:00:15Z"));
        final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        final TopeEnvios tope = new TopeEnvios(3, 10, clock, meterRegistry);

        assertTrue(tope.disponible());
        assertTrue(tope.intentarConsumir());
        assertTrue(tope.intentarConsumir());
        assertTrue(tope.intentarConsumir());

        // Tope de minuto alcanzado (3 de 3)
        assertFalse(tope.disponible());
        assertFalse(tope.intentarConsumir());
        assertEquals(1.0, meterRegistry.counter("notification.email.throttled.total", "window", "minute").count());

        // Avanzar al siguiente minuto -> se libera cupo
        clock.advance(Duration.ofSeconds(46));
        assertTrue(tope.disponible());
        assertTrue(tope.intentarConsumir());
    }

    @Test
    @DisplayName("BE-K02: tope diario independiente del minuto")
    void BE_K02_tope_diario_independiente_del_minuto() {
        final MutableClock clock = new MutableClock(Instant.parse("2026-10-30T10:00:00Z"));
        final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        final TopeEnvios tope = new TopeEnvios(2, 3, clock, meterRegistry);

        // Minuto 1: consume 2
        assertTrue(tope.intentarConsumir());
        assertTrue(tope.intentarConsumir());
        assertFalse(tope.disponible());

        // Minuto 2: consume el 3ro (alcanza tope diario de 3)
        clock.advance(Duration.ofMinutes(1));
        assertTrue(tope.disponible());
        assertTrue(tope.intentarConsumir());

        // Aunque avance otro minuto, el tope diario sigue agotado
        clock.advance(Duration.ofMinutes(1));
        assertFalse(tope.disponible());
        assertFalse(tope.intentarConsumir());
        assertEquals(1.0, meterRegistry.counter("notification.email.throttled.total", "window", "day").count());

        // Al avanzar al dia siguiente, se libera
        clock.advance(Duration.ofHours(14));
        assertTrue(tope.disponible());
        assertTrue(tope.intentarConsumir());
    }

    @Test
    @DisplayName("BE-K03: concurrencia: N hilos no superan el tope")
    void BE_K03_concurrencia_n_hilos_no_superan_tope() throws Exception {
        final MutableClock clock = new MutableClock(Instant.parse("2026-10-30T12:00:00Z"));
        final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        final TopeEnvios tope = new TopeEnvios(15, 50, clock, meterRegistry);

        final int numThreads = 40;
        final ExecutorService pool = Executors.newFixedThreadPool(10);
        final AtomicInteger exitos = new AtomicInteger(0);
        try {
            final List<Callable<Void>> tareas = new ArrayList<>();
            for (int i = 0; i < numThreads; i++) {
                tareas.add(() -> {
                    if (tope.intentarConsumir()) {
                        exitos.incrementAndGet();
                    }
                    return null;
                });
            }
            final List<Future<Void>> futuros = pool.invokeAll(tareas);
            for (final Future<Void> f : futuros) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(15, exitos.get());
        assertFalse(tope.disponible());

        assertThrows(IllegalArgumentException.class, () -> new TopeEnvios(0, 10, clock, meterRegistry));
        assertThrows(IllegalArgumentException.class, () -> new TopeEnvios(5, 4, clock, meterRegistry));
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> current;

        private MutableClock(final Instant initial) {
            this.current = new AtomicReference<>(initial);
        }

        void advance(final Duration duration) {
            current.updateAndGet(now -> now.plus(duration));
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current.get();
        }
    }
}
