package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler;

import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.ToIntFunction;

/**
 * Job diario del SERVIDOR que purga {@code dbo.AuditoriaEvento} (mismo patron que {@link CargaFestivosScheduler}:
 * {@link SmartLifecycle}, hilo daemon, sin {@code @Scheduled}). La auditoria conserva su contrato aparte del resto de
 * la arquitectura (ver docs/observability/README.md), asi que este job depende directamente del repositorio JPA de
 * auditoria en vez de un puerto de Application: no hay caso de uso de negocio detras de una purga de retencion.
 * <p>
 * Cada pasada abre su propia correlacion, nunca propaga excepciones, y recalcula el limite ({@code ahora - retencion})
 * en cada ejecucion (no en el constructor) para que la retencion sea siempre relativa al momento de la purga.
 */
public final class PurgaAuditoriaScheduler implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(PurgaAuditoriaScheduler.class);

    /** {@code int purgarAnterioresA(OffsetDateTime limite)} — desacoplado del repositorio concreto para la prueba. */
    private final ToIntFunction<OffsetDateTime> purgarAnterioresA;
    private final Clock clock;
    private final Duration retencion;
    private final Duration retardoInicial;
    private final Duration tick;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService executor;

    public PurgaAuditoriaScheduler(
            final ToIntFunction<OffsetDateTime> purgarAnterioresA,
            final Clock clock,
            final Duration retencion,
            final Duration retardoInicial,
            final Duration tick
    ) {
        this.purgarAnterioresA = Objects.requireNonNull(purgarAnterioresA, "La operacion de purga es obligatoria.");
        this.clock = Objects.requireNonNull(clock, "El reloj es obligatorio.");
        this.retencion = Objects.requireNonNull(retencion, "La retencion de auditoria es obligatoria.");
        this.retardoInicial = Objects.requireNonNull(retardoInicial, "El retardo inicial del scheduler es obligatorio.");
        this.tick = Objects.requireNonNull(tick, "El tick del scheduler es obligatorio.");
        if (retencion.isZero() || retencion.isNegative()) {
            throw new IllegalArgumentException("La retencion de auditoria debe ser positiva.");
        }
        if (tick.isZero() || tick.isNegative()) {
            throw new IllegalArgumentException("El tick del scheduler de purga de auditoria debe ser positivo.");
        }
        if (retardoInicial.isNegative()) {
            throw new IllegalArgumentException("El retardo inicial del scheduler de purga de auditoria no puede ser negativo.");
        }
    }

    /** Una pasada completa; visible para pruebas. */
    public void ejecutarPasada() {
        final boolean correlacionPropia = CorrelationIdContext.get() == null;
        if (correlacionPropia) {
            CorrelationIdContext.set(UUID.randomUUID());
        }
        try {
            final OffsetDateTime limite = OffsetDateTime.now(clock).minus(retencion);
            final int eliminados = purgarAnterioresA.applyAsInt(limite);
            if (eliminados > 0) {
                LOGGER.info("Pasada de purga de auditoria completada. eliminados={}, limite={}", eliminados, limite);
            }
        } catch (final RuntimeException exception) {
            LOGGER.error("Fallo la pasada de purga de auditoria: {}", exception.getClass().getSimpleName());
        } finally {
            if (correlacionPropia) {
                CorrelationIdContext.clear();
            }
        }
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(tarea -> {
            final Thread hilo = new Thread(tarea, "purga-auditoria");
            hilo.setDaemon(true);
            return hilo;
        });
        executor.scheduleWithFixedDelay(this::ejecutarPasada, retardoInicial.toMillis(), tick.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false) && executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}
