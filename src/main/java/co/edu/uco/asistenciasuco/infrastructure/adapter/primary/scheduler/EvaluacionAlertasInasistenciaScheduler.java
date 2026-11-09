package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler;

import co.edu.uco.asistenciasuco.application.features.alertainasistencia.evaluaralertasautomatico.primaryports.EvaluarAlertasInasistenciaAutomaticoInputPort;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CULM-001 C.1 (HU026C, contrato 3.4): job del SERVIDOR que evalua las alertas de inasistencia al vencer las sesiones y recalcula los
 * resolutores de las revisiones pendientes. No es polling del cliente: el estudiante, el docente y los resolutores se enteran por SSE
 * y correo. Cada pasada abre su propia correlacion, nunca propaga excepciones y es idempotente entre instancias (marca de agua por grupo
 * y cerrojo en la DB).
 */
public final class EvaluacionAlertasInasistenciaScheduler implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluacionAlertasInasistenciaScheduler.class);

    private final EvaluarAlertasInasistenciaAutomaticoInputPort evaluar;
    private final Duration tick;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService executor;

    public EvaluacionAlertasInasistenciaScheduler(final EvaluarAlertasInasistenciaAutomaticoInputPort evaluar, final Duration tick) {
        this.evaluar = Objects.requireNonNull(evaluar, "EvaluarAlertasInasistenciaAutomaticoInputPort es obligatorio.");
        this.tick = Objects.requireNonNull(tick, "El tick del scheduler es obligatorio.");
        if (tick.isZero() || tick.isNegative()) {
            throw new IllegalArgumentException("El tick del scheduler debe ser positivo.");
        }
    }

    /** Una pasada completa; visible para pruebas. */
    public void ejecutarPasada() {
        final boolean correlacionPropia = CorrelationIdContext.get() == null;
        if (correlacionPropia) {
            CorrelationIdContext.set(UUID.randomUUID());
        }
        try {
            evaluar.execute();
        } catch (final RuntimeException exception) {
            LOGGER.error("Fallo la pasada de evaluacion de alertas de inasistencia: {}", exception.getClass().getSimpleName());
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
            final Thread hilo = new Thread(tarea, "evaluacion-alertas-inasistencia");
            hilo.setDaemon(true);
            return hilo;
        });
        executor.scheduleWithFixedDelay(this::ejecutarPasada, tick.toMillis(), tick.toMillis(), TimeUnit.MILLISECONDS);
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
