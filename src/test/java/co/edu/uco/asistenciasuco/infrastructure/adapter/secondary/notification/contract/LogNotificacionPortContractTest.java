package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contract;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.LogNotificacionAdapter;
import org.junit.jupiter.api.BeforeEach;

public class LogNotificacionPortContractTest extends NotificacionPortContractTest {

    private LogNotificacionAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new LogNotificacionAdapter();
    }

    @Override
    protected NotificacionPort puerto() {
        return adapter;
    }

    @Override
    protected void simularFalloInfraestructura() {
        // Log adapter no tiene dependencias de infraestructura externas que puedan fallar.
    }
}
