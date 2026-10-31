package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.composite;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.LogNotificacionAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.OutboxNotificacionAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters.NotificationAdapterProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters.NotificationAdapterProperties.Composite;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters.NotificationAdapterProperties.Provider;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters.NotificationAdapterProperties.Target;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompositeNotificacionAdapterConfigurationTest {

    private final CompositeNotificacionAdapterConfiguration configuration = new CompositeNotificacionAdapterConfiguration();

    @Test
    @DisplayName("compositeNotificacionPort falla si logProvider devuelve null")
    void composite_falla_si_log_provider_nulo() {
        final NotificationAdapterProperties props = new NotificationAdapterProperties(
                Provider.COMPOSITE,
                new Composite(List.of(Target.LOG, Target.OUTBOX))
        );

        final ObjectProvider<LogNotificacionAdapter> logProvider = mock(ObjectProvider.class);
        final ObjectProvider<OutboxNotificacionAdapter> outboxProvider = mock(ObjectProvider.class);
        when(logProvider.getIfAvailable()).thenReturn(null);

        final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        assertThrows(IllegalStateException.class, () -> configuration.compositeNotificacionPort(
                props, logProvider, outboxProvider, meterRegistry
        ));
    }

    @Test
    @DisplayName("compositeNotificacionPort falla si outboxProvider devuelve null")
    void composite_falla_si_outbox_provider_nulo() {
        final NotificationAdapterProperties props = new NotificationAdapterProperties(
                Provider.COMPOSITE,
                new Composite(List.of(Target.LOG, Target.OUTBOX))
        );

        final ObjectProvider<LogNotificacionAdapter> logProvider = mock(ObjectProvider.class);
        final ObjectProvider<OutboxNotificacionAdapter> outboxProvider = mock(ObjectProvider.class);
        when(logProvider.getIfAvailable()).thenReturn(new LogNotificacionAdapter());
        when(outboxProvider.getIfAvailable()).thenReturn(null);

        final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        assertThrows(IllegalStateException.class, () -> configuration.compositeNotificacionPort(
                props, logProvider, outboxProvider, meterRegistry
        ));
    }

    @Test
    @DisplayName("compositeNotificacionPort con composite nulo crea lista de targets vacia")
    void composite_con_propiedades_nulas() {
        final NotificationAdapterProperties props = new NotificationAdapterProperties(
                Provider.LOG,
                null
        );

        final ObjectProvider<LogNotificacionAdapter> logProvider = mock(ObjectProvider.class);
        final ObjectProvider<OutboxNotificacionAdapter> outboxProvider = mock(ObjectProvider.class);

        final NotificacionPort port = configuration.compositeNotificacionPort(
                props, logProvider, outboxProvider, new SimpleMeterRegistry()
        );
        assertNotNull(port);
    }

    @Test
    @DisplayName("compositeNotificacionPort happy path con LOG y OUTBOX disponibles")
    void composite_happy_path_log_y_outbox() {
        final NotificationAdapterProperties props = new NotificationAdapterProperties(
                Provider.COMPOSITE,
                new Composite(List.of(Target.LOG, Target.OUTBOX))
        );

        final ObjectProvider<LogNotificacionAdapter> logProvider = mock(ObjectProvider.class);
        final ObjectProvider<OutboxNotificacionAdapter> outboxProvider = mock(ObjectProvider.class);
        when(logProvider.getIfAvailable()).thenReturn(new LogNotificacionAdapter());
        when(outboxProvider.getIfAvailable()).thenReturn(mock(OutboxNotificacionAdapter.class));

        final NotificacionPort port = configuration.compositeNotificacionPort(
                props, logProvider, outboxProvider, new SimpleMeterRegistry()
        );
        assertNotNull(port);
    }
}
