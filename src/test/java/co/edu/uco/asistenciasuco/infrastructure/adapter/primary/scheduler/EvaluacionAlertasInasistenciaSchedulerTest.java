package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler;

import co.edu.uco.asistenciasuco.application.features.alertainasistencia.evaluaralertasautomatico.primaryports.EvaluarAlertasInasistenciaAutomaticoInputPort;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 C.1 (contrato 3.4): arranque y parada, correlacion propia por pasada y un fallo que no detiene el job. */
class EvaluacionAlertasInasistenciaSchedulerTest {

    @Test
    void la_pasada_abre_su_correlacion_y_la_limpia() {
        final AtomicReference<UUID> vista = new AtomicReference<>();
        final EvaluarAlertasInasistenciaAutomaticoInputPort evaluar = () -> {
            vista.set(CorrelationIdContext.get());
            return 1;
        };
        new EvaluacionAlertasInasistenciaScheduler(evaluar, Duration.ofSeconds(1)).ejecutarPasada();
        assertNotNull(vista.get());
        assertNull(CorrelationIdContext.get());
    }

    @Test
    void conserva_la_correlacion_existente_y_un_fallo_no_se_propaga() {
        final UUID existente = UUID.randomUUID();
        CorrelationIdContext.set(existente);
        try {
            final EvaluarAlertasInasistenciaAutomaticoInputPort evaluar = mock(EvaluarAlertasInasistenciaAutomaticoInputPort.class);
            when(evaluar.execute()).thenThrow(new IllegalStateException("db"));
            new EvaluacionAlertasInasistenciaScheduler(evaluar, Duration.ofSeconds(1)).ejecutarPasada();
            verify(evaluar, times(1)).execute();
            assertEquals(existente, CorrelationIdContext.get());
        } finally {
            CorrelationIdContext.clear();
        }
    }

    @Test
    void arranca_ejecuta_y_se_detiene() throws InterruptedException {
        final CountDownLatch pasada = new CountDownLatch(1);
        final EvaluacionAlertasInasistenciaScheduler scheduler = new EvaluacionAlertasInasistenciaScheduler(() -> {
            pasada.countDown();
            return 0;
        }, Duration.ofMillis(20));
        assertFalse(scheduler.isRunning());
        scheduler.start();
        scheduler.start();
        assertTrue(scheduler.isRunning());
        assertTrue(pasada.await(2, TimeUnit.SECONDS));
        scheduler.stop();
        scheduler.stop();
        assertFalse(scheduler.isRunning());
    }

    @Test
    void valida_sus_argumentos() {
        final EvaluarAlertasInasistenciaAutomaticoInputPort evaluar = () -> 0;
        assertThrows(NullPointerException.class, () -> new EvaluacionAlertasInasistenciaScheduler(null, Duration.ofSeconds(1)));
        assertThrows(NullPointerException.class, () -> new EvaluacionAlertasInasistenciaScheduler(evaluar, null));
        assertThrows(IllegalArgumentException.class, () -> new EvaluacionAlertasInasistenciaScheduler(evaluar, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new EvaluacionAlertasInasistenciaScheduler(evaluar, Duration.ofSeconds(-1)));
    }
}
