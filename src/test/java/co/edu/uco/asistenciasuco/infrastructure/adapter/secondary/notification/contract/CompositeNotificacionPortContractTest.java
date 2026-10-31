package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contract;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.composite.CompositeNotificacionAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.composite.CompositeNotificacionAdapter.NamedTarget;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class CompositeNotificacionPortContractTest extends NotificacionPortContractTest {

    private NotificacionPort target1;
    private NotificacionPort target2;
    private CompositeNotificacionAdapter adapter;

    @BeforeEach
    void setUp() {
        target1 = mock(NotificacionPort.class);
        target2 = mock(NotificacionPort.class);
        adapter = new CompositeNotificacionAdapter(
                List.of(new NamedTarget("target1", target1), new NamedTarget("target2", target2)),
                new SimpleMeterRegistry()
        );
    }

    @Override
    protected NotificacionPort puerto() {
        return adapter;
    }

    @Override
    protected void simularFalloInfraestructura() {
        doThrow(new RuntimeException("Simulated target failure")).when(target1).notificar(any());
        doThrow(new RuntimeException("Simulated target failure")).when(target2).notificar(any());
    }
}
