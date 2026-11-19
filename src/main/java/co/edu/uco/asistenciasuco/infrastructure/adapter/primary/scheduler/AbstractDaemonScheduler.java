package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler;

import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Infraestructura compartida para jobs periodicos del servidor: hilo daemon, ciclo de vida Spring y correlacion propia
 * por pasada. Las tareas concretas conservan sus operaciones y su politica de registro de errores.
 */
public abstract class AbstractDaemonScheduler implements SmartLifecycle {

    private final String threadName;
    private final Duration initialDelay;
    private final Duration tick;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService executor;

    protected AbstractDaemonScheduler(
            final String threadName,
            final Duration initialDelay,
            final Duration tick
    ) {
        this.threadName = Objects.requireNonNull(threadName, "El nombre del hilo del scheduler es obligatorio.");
        this.initialDelay = Objects.requireNonNull(initialDelay, "El retardo inicial del scheduler es obligatorio.");
        this.tick = Objects.requireNonNull(tick, "El tick del scheduler es obligatorio.");
        if (threadName.isBlank()) {
            throw new IllegalArgumentException("El nombre del hilo del scheduler no puede estar vacio.");
        }
        if (tick.isZero() || tick.isNegative()) {
            throw new IllegalArgumentException("El tick del scheduler debe ser positivo.");
        }
        if (initialDelay.isNegative()) {
            throw new IllegalArgumentException("El retardo inicial del scheduler no puede ser negativo.");
        }
    }

    @Override
    public final void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(tarea -> {
            final Thread hilo = new Thread(tarea, threadName);
            hilo.setDaemon(true);
            return hilo;
        });
        executor.scheduleWithFixedDelay(this::ejecutarPasada, initialDelay.toMillis(), tick.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public final void stop() {
        if (running.compareAndSet(true, false) && executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public final boolean isRunning() {
        return running.get();
    }

    protected final void ejecutarConCorrelacion(
            final Runnable operacion,
            final Consumer<RuntimeException> alFallar
    ) {
        final boolean correlacionPropia = CorrelationIdContext.get() == null;
        if (correlacionPropia) {
            CorrelationIdContext.set(java.util.UUID.randomUUID());
        }
        try {
            operacion.run();
        } catch (final RuntimeException exception) {
            alFallar.accept(exception);
        } finally {
            if (correlacionPropia) {
                CorrelationIdContext.clear();
            }
        }
    }

    protected abstract void ejecutarPasada();
}
