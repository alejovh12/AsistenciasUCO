package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler;

import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Job diario de purga de auditoria: correlacion propia por pasada, limite relativo a "ahora" y sin propagar fallos. */
class PurgaAuditoriaSchedulerTest {

    private static final Clock RELOJ_FIJO = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC);

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void cada_pasada_abre_y_cierra_su_propia_correlacion() {
        final AtomicReference<UUID> vista = new AtomicReference<>();
        final PurgaAuditoriaScheduler scheduler = new PurgaAuditoriaScheduler(limite -> {
            vista.set(CorrelationIdContext.get());
            return 0;
        }, RELOJ_FIJO, Duration.ofDays(4), Duration.ofMinutes(1), Duration.ofHours(24));

        scheduler.ejecutarPasada();

        assertNotNull(vista.get());
        assertNull(CorrelationIdContext.get());
    }

    @Test
    void calcula_el_limite_como_ahora_menos_la_retencion() {
        final AtomicReference<OffsetDateTime> limiteVisto = new AtomicReference<>();
        final PurgaAuditoriaScheduler scheduler = new PurgaAuditoriaScheduler(limite -> {
            limiteVisto.set(limite);
            return 3;
        }, RELOJ_FIJO, Duration.ofDays(4), Duration.ZERO, Duration.ofHours(24));

        scheduler.ejecutarPasada();

        assertEquals(OffsetDateTime.now(RELOJ_FIJO).minusDays(4), limiteVisto.get());
    }

    @Test
    void conserva_una_correlacion_existente_y_no_propaga_excepciones() {
        final UUID existente = UUID.randomUUID();
        CorrelationIdContext.set(existente);
        final AtomicReference<UUID> vista = new AtomicReference<>();
        new PurgaAuditoriaScheduler(limite -> {
            vista.set(CorrelationIdContext.get());
            throw new IllegalStateException("db caida");
        }, RELOJ_FIJO, Duration.ofDays(4), Duration.ZERO, Duration.ofHours(24)).ejecutarPasada();

        assertEquals(existente, vista.get());
        assertEquals(existente, CorrelationIdContext.get());
        new PurgaAuditoriaScheduler(limite -> 0, RELOJ_FIJO, Duration.ofDays(4), Duration.ZERO, Duration.ofHours(24))
                .ejecutarPasada();
    }

    @Test
    void start_y_stop_son_idempotentes() {
        final PurgaAuditoriaScheduler scheduler = new PurgaAuditoriaScheduler(
                limite -> 0, RELOJ_FIJO, Duration.ofDays(4), Duration.ofHours(1), Duration.ofHours(24));
        assertFalse(scheduler.isRunning());
        scheduler.start();
        scheduler.start();
        assertTrue(scheduler.isRunning());
        scheduler.stop();
        scheduler.stop();
        assertFalse(scheduler.isRunning());
        new PurgaAuditoriaScheduler(limite -> 0, RELOJ_FIJO, Duration.ofDays(4), Duration.ofHours(1), Duration.ofHours(24))
                .stop();
    }

    @Test
    void valida_sus_argumentos() {
        final var puerto = (java.util.function.ToIntFunction<OffsetDateTime>) limite -> 0;
        final var unaHora = Duration.ofHours(1);

        assertThrows(NullPointerException.class,
                () -> new PurgaAuditoriaScheduler(null, RELOJ_FIJO, Duration.ofDays(4), Duration.ZERO, unaHora));
        assertThrows(NullPointerException.class,
                () -> new PurgaAuditoriaScheduler(puerto, null, Duration.ofDays(4), Duration.ZERO, unaHora));
        assertThrows(NullPointerException.class,
                () -> new PurgaAuditoriaScheduler(puerto, RELOJ_FIJO, null, Duration.ZERO, unaHora));
        assertThrows(NullPointerException.class,
                () -> new PurgaAuditoriaScheduler(puerto, RELOJ_FIJO, Duration.ofDays(4), null, unaHora));
        assertThrows(NullPointerException.class,
                () -> new PurgaAuditoriaScheduler(puerto, RELOJ_FIJO, Duration.ofDays(4), Duration.ZERO, null));

        assertThrows(IllegalArgumentException.class, () -> new PurgaAuditoriaScheduler(
                puerto, RELOJ_FIJO, Duration.ZERO, Duration.ZERO, unaHora));
        assertThrows(IllegalArgumentException.class, () -> new PurgaAuditoriaScheduler(
                puerto, RELOJ_FIJO, Duration.ofSeconds(-1), Duration.ZERO, unaHora));
        assertThrows(IllegalArgumentException.class, () -> new PurgaAuditoriaScheduler(
                puerto, RELOJ_FIJO, Duration.ofDays(4), Duration.ZERO, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new PurgaAuditoriaScheduler(
                puerto, RELOJ_FIJO, Duration.ofDays(4), Duration.ofSeconds(-1), unaHora));
    }
}
