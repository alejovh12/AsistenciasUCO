package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler;

import co.edu.uco.asistenciasuco.application.features.alertainasistencia.evaluaralertasautomatico.primaryports.EvaluarAlertasInasistenciaAutomaticoInputPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;

/**
 * CULM-001 C.1 (HU026C, contrato 3.4): job del SERVIDOR que evalua las alertas de inasistencia al vencer las sesiones
 * y recalcula los resolutores de las revisiones pendientes. No es polling del cliente: el estudiante, el docente y los
 * resolutores se enteran por SSE y correo. Cada pasada abre su propia correlacion, nunca propaga excepciones y es
 * idempotente entre instancias (marca de agua por grupo y cerrojo en la DB).
 */
public final class EvaluacionAlertasInasistenciaScheduler extends AbstractDaemonScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluacionAlertasInasistenciaScheduler.class);

    private final EvaluarAlertasInasistenciaAutomaticoInputPort evaluar;

    public EvaluacionAlertasInasistenciaScheduler(
            final EvaluarAlertasInasistenciaAutomaticoInputPort evaluar,
            final Duration tick
    ) {
        super("evaluacion-alertas-inasistencia", tick, tick);
        this.evaluar = Objects.requireNonNull(evaluar, "EvaluarAlertasInasistenciaAutomaticoInputPort es obligatorio.");
    }

    /** Una pasada completa; visible para pruebas. */
    @Override
    public void ejecutarPasada() {
        ejecutarConCorrelacion(evaluar::execute,
                exception -> LOGGER.error("Fallo la pasada de evaluacion de alertas de inasistencia: {}",
                        exception.getClass().getSimpleName()));
    }
}
