package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.composite;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.composite.CompositeNotificacionAdapter.NamedTarget;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CompositeNotificacionAdapterTest {

    private NotificacionPort target1;
    private NotificacionPort target2;
    private MeterRegistry meterRegistry;
    private CompositeNotificacionAdapter compositeAdapter;

    @BeforeEach
    void setUp() {
        target1 = mock(NotificacionPort.class);
        target2 = mock(NotificacionPort.class);
        meterRegistry = new SimpleMeterRegistry();
        compositeAdapter = new CompositeNotificacionAdapter(
                List.of(new NamedTarget("log", target1), new NamedTarget("outbox", target2)),
                meterRegistry
        );
    }

    @Test
    @DisplayName("BE-C01: invoca todos los destinos en orden")
    void BE_C01_invoca_destinos_en_orden() {
        final Notificacion notificacion = new Notificacion(
                "TEST", "Asunto", "Mensaje",
                List.of(new Notificacion.Destinatario("ROL", UUID.randomUUID(), "N", "c@uco.edu.co")),
                Map.of()
        );

        compositeAdapter.notificar(notificacion);

        final InOrder inOrder = inOrder(target1, target2);
        inOrder.verify(target1).notificar(notificacion);
        inOrder.verify(target2).notificar(notificacion);
    }

    @Test
    @DisplayName("BE-C02: un destino lanza RuntimeException, los demas se invocan y failures incrementa")
    void BE_C02_un_destino_falla_continua_con_los_demas() {
        doThrow(new RuntimeException("Error en target1")).when(target1).notificar(any());

        final Notificacion notificacion = new Notificacion(
                "TEST", "Asunto", "Mensaje", List.of(), Map.of()
        );

        assertDoesNotThrow(() -> compositeAdapter.notificar(notificacion));
        verify(target2).notificar(notificacion);

        final double failures = meterRegistry.get("notification.composite.target.failures")
                .tag("target", "log")
                .counter().count();
        assertEquals(1.0, failures);
    }

    @Test
    @DisplayName("BE-C03: todos los destinos fallan y no se propaga excepcion")
    void BE_C03_todos_fallan_no_propaga() {
        doThrow(new RuntimeException("Error target 1")).when(target1).notificar(any());
        doThrow(new RuntimeException("Error target 2")).when(target2).notificar(any());

        final Notificacion notificacion = new Notificacion(
                "TEST", "Asunto", "Mensaje", List.of(), Map.of()
        );

        assertDoesNotThrow(() -> compositeAdapter.notificar(notificacion));
    }

    @Test
    @DisplayName("BE-C04: notificar(null) lanza NullPointerException y ningun destino es invocado")
    void BE_C04_notificacion_nula_lanza_npe_sin_invocar_destinos() {
        assertThrows(NullPointerException.class, () -> compositeAdapter.notificar(null));
        verify(target1, never()).notificar(any());
        verify(target2, never()).notificar(any());
    }

    @Test
    @DisplayName("BE-C05: el mismo objeto Notificacion llega a cada destino")
    void BE_C05_mismo_objeto_notificacion() {
        final Notificacion notificacion = new Notificacion(
                "TEST", "Asunto", "Mensaje", List.of(), Map.of()
        );

        compositeAdapter.notificar(notificacion);

        verify(target1).notificar(notificacion);
        verify(target2).notificar(notificacion);
    }
}
