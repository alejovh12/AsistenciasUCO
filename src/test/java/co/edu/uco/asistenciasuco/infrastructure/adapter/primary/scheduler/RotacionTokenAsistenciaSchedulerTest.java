package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler;

import co.edu.uco.asistenciasuco.application.features.asistenciaqr.rotartokensasistenciaautomatico.primaryports.RotarTokensAsistenciaAutomaticoInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.avisarfaltassesion.primaryports.AvisarFaltasSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartomasvencidas.primaryports.CerrarTomasVencidasInputPort;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 B.2 (CONTRACT 3.5) y ATA D1 (HU175): job de rotacion, cierre automatico y aviso de faltas, con correlacion propia y sin propagar fallos. */
class RotacionTokenAsistenciaSchedulerTest {

    private final RotarTokensAsistenciaAutomaticoInputPort rotar = mock(RotarTokensAsistenciaAutomaticoInputPort.class);
    private final CerrarTomasVencidasInputPort cerrar = mock(CerrarTomasVencidasInputPort.class);
    private final AvisarFaltasSesionInputPort avisar = mock(AvisarFaltasSesionInputPort.class);

    @Test
    void cada_pasada_rota_cierra_y_avisa_aunque_la_rotacion_falle_y_limpia_su_correlacion() {
        when(rotar.execute()).thenThrow(new IllegalStateException("db"));
        final RotacionTokenAsistenciaScheduler scheduler = new RotacionTokenAsistenciaScheduler(rotar, cerrar, avisar, Duration.ofSeconds(5));

        scheduler.ejecutarPasada();

        verify(rotar).execute();
        verify(cerrar).execute();
        verify(avisar).execute();
        assertNull(CorrelationIdContext.get());
    }

    @Test
    void el_aviso_de_faltas_corre_despues_del_cierre_y_su_fallo_no_se_propaga() {
        when(avisar.execute()).thenThrow(new IllegalStateException("db"));
        final RotacionTokenAsistenciaScheduler scheduler = new RotacionTokenAsistenciaScheduler(rotar, cerrar, avisar, Duration.ofSeconds(5));

        scheduler.ejecutarPasada();

        final var orden = inOrder(rotar, cerrar, avisar);
        orden.verify(rotar).execute();
        orden.verify(cerrar).execute();
        orden.verify(avisar).execute();
        assertNull(CorrelationIdContext.get());
    }

    @Test
    void un_fallo_del_cierre_no_impide_el_aviso_de_faltas() {
        when(cerrar.execute()).thenThrow(new IllegalStateException("db"));

        new RotacionTokenAsistenciaScheduler(rotar, cerrar, avisar, Duration.ofSeconds(5)).ejecutarPasada();

        verify(avisar).execute();
    }

    @Test
    void conserva_una_correlacion_existente() {
        final UUID existente = UUID.randomUUID();
        CorrelationIdContext.set(existente);
        try {
            when(cerrar.execute()).thenThrow(new IllegalStateException("db"));
            new RotacionTokenAsistenciaScheduler(rotar, cerrar, avisar, Duration.ofSeconds(5)).ejecutarPasada();
            assertEquals(existente, CorrelationIdContext.get());
        } finally {
            CorrelationIdContext.clear();
        }
    }

    @Test
    void ciclo_de_vida_con_hilo_daemon_y_validaciones() throws Exception {
        final CountDownLatch pasada = new CountDownLatch(1);
        when(cerrar.execute()).thenAnswer(invocacion -> {
            pasada.countDown();
            return 0;
        });
        final RotacionTokenAsistenciaScheduler scheduler = new RotacionTokenAsistenciaScheduler(rotar, cerrar, avisar, Duration.ofMillis(20));
        scheduler.start();
        scheduler.start();
        assertTrue(scheduler.isRunning());
        assertTrue(pasada.await(5, TimeUnit.SECONDS));
        scheduler.stop();
        scheduler.stop();
        assertFalse(scheduler.isRunning());

        final var duration3 = Duration.ofSeconds(1);
        assertThrows(NullPointerException.class, () -> new RotacionTokenAsistenciaScheduler(null, cerrar, avisar, duration3));
        final var duration2 = Duration.ofSeconds(1);
        assertThrows(NullPointerException.class, () -> new RotacionTokenAsistenciaScheduler(rotar, null, avisar, duration2));
        assertThrows(NullPointerException.class, () -> new RotacionTokenAsistenciaScheduler(rotar, cerrar, null, duration2));
        assertThrows(NullPointerException.class, () -> new RotacionTokenAsistenciaScheduler(rotar, cerrar, avisar, null));
        assertThrows(IllegalArgumentException.class, () -> new RotacionTokenAsistenciaScheduler(rotar, cerrar, avisar, Duration.ZERO));
        final var duration = Duration.ofSeconds(-1);
        assertThrows(IllegalArgumentException.class, () -> new RotacionTokenAsistenciaScheduler(rotar, cerrar, avisar, duration));
    }
}
