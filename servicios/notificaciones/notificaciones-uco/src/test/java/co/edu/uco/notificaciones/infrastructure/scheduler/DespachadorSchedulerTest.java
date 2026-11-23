package co.edu.uco.notificaciones.infrastructure.scheduler;

import co.edu.uco.notificaciones.application.model.ConfiguracionDespachador;
import co.edu.uco.notificaciones.application.service.Despachador;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DespachadorSchedulerTest {

    private static ConfiguracionDespachador config(final boolean habilitado, final Duration barrido) {
        return new ConfiguracionDespachador(habilitado, 5, Duration.ofSeconds(60), barrido, 5, Duration.ofSeconds(30), Duration.ofMinutes(15), 30,
                Duration.ofHours(6), "inst");
    }

    @Test
    void deshabilitadoNoArrancaElHilo() {
        final Despachador d = mock(Despachador.class);
        when(d.configuracion()).thenReturn(config(false, Duration.ofMillis(50)));
        final DespachadorScheduler s = new DespachadorScheduler(d);
        s.start();
        assertFalse(s.isRunning());
        verify(d, never()).depurar();
        s.stop();
    }

    @Test
    void barreDespiertaYSeDetieneLimpiamente() throws Exception {
        final Despachador d = mock(Despachador.class);
        when(d.configuracion()).thenReturn(config(true, Duration.ofHours(1)));
        final DespachadorScheduler s = new DespachadorScheduler(d);
        s.start();
        s.start();
        assertTrue(s.isRunning());
        assertEquals(Integer.MAX_VALUE - 100, s.getPhase());
        verify(d, timeout(3000).atLeast(1)).ejecutarCiclo(any(BooleanSupplier.class));
        s.despertar();
        verify(d, timeout(3000).atLeast(2)).ejecutarCiclo(any(BooleanSupplier.class));
        s.stop();
        assertFalse(s.isRunning());
        s.stop();
        verify(d, atLeast(1)).depurar();
    }

    @Test
    void unCicloQueFallaNoMataElHilo() {
        final Despachador d = mock(Despachador.class);
        when(d.configuracion()).thenReturn(config(true, Duration.ofMillis(20)));
        when(d.ejecutarCiclo(any(BooleanSupplier.class))).thenThrow(new IllegalStateException("bd")).thenReturn(0);
        final DespachadorScheduler s = new DespachadorScheduler(d);
        s.start();
        verify(d, timeout(3000).atLeast(3)).ejecutarCiclo(any(BooleanSupplier.class));
        s.stop();
    }

    @Test
    void exigeElDespachador() {
        assertThrows(NullPointerException.class, () -> new DespachadorScheduler(null));
    }
}
