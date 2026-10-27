package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contract;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionOutboxDispatcher;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionOutboxRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.OutboxNotificacionAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class OutboxNotificacionPortContractTest extends NotificacionPortContractTest {

    private NotificacionOutboxRepository repository;
    private NotificacionOutboxDispatcher dispatcher;
    private OutboxNotificacionAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(NotificacionOutboxRepository.class);
        dispatcher = mock(NotificacionOutboxDispatcher.class);
        adapter = new OutboxNotificacionAdapter(repository, dispatcher, new SimpleMeterRegistry(), new ObjectMapper());
    }

    @Override
    protected NotificacionPort puerto() {
        return adapter;
    }

    @Override
    protected void simularFalloInfraestructura() {
        doThrow(new RuntimeException("Simulated database failure")).when(repository).registrar(any());
    }
}
