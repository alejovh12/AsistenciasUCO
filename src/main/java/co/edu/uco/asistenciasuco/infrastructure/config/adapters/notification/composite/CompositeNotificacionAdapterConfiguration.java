package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.composite;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.LogNotificacionAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.composite.CompositeNotificacionAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.composite.CompositeNotificacionAdapter.NamedTarget;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.OutboxNotificacionAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters.NotificationAdapterProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters.NotificationAdapterProperties.Target;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

/**
 * Composition Root del adaptador composite para notificaciones.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(NotificationAdapterProperties.class)
@ConditionalOnProperty(
        prefix = "app.adapters.notification",
        name = "provider",
        havingValue = "composite"
)
public class CompositeNotificacionAdapterConfiguration {

    @Bean
    @Primary
    public NotificacionPort compositeNotificacionPort(
            final NotificationAdapterProperties properties,
            final ObjectProvider<LogNotificacionAdapter> logProvider,
            final ObjectProvider<OutboxNotificacionAdapter> outboxProvider,
            final MeterRegistry meterRegistry
    ) {
        final List<NamedTarget> targets = new ArrayList<>();
        if (properties.composite() != null && properties.composite().targets() != null) {
            for (final Target target : properties.composite().targets()) {
                switch (target) {
                    case LOG -> {
                        final LogNotificacionAdapter logAdapter = logProvider.getIfAvailable();
                        if (logAdapter == null) {
                            throw new IllegalStateException("LogNotificacionAdapter no disponible para composite.");
                        }
                        targets.add(new NamedTarget("log", logAdapter));
                    }
                    case OUTBOX -> {
                        final OutboxNotificacionAdapter outboxAdapter = outboxProvider.getIfAvailable();
                        if (outboxAdapter == null) {
                            throw new IllegalStateException("OutboxNotificacionAdapter no disponible para composite.");
                        }
                        targets.add(new NamedTarget("outbox", outboxAdapter));
                    }
                }
            }
        }
        return new CompositeNotificacionAdapter(targets, meterRegistry);
    }
}
