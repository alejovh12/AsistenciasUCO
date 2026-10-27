package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.outbox;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.LogNotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionOutboxDispatcher;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionOutboxRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.OutboxNotificacionAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.condition.OutboxAdapterCondition;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters.NotificationAdapterProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.OutboxNotificationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Composition Root del proveedor outbox de notificaciones.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({NotificationAdapterProperties.class, OutboxNotificationProperties.class})
public class OutboxNotificacionAdapterConfiguration {

    @Bean
    @Conditional(OutboxAdapterCondition.class)
    public OutboxNotificacionAdapter outboxNotificacionAdapter(
            final NotificacionOutboxRepository repository,
            final NotificacionOutboxDispatcher dispatcher,
            final MeterRegistry meterRegistry,
            final ObjectMapper objectMapper
    ) {
        return new OutboxNotificacionAdapter(repository, dispatcher, meterRegistry, objectMapper);
    }

    @Bean
    @Conditional(OutboxAdapterCondition.class)
    public NotificacionOutboxDispatcher notificacionOutboxDispatcher(
            final NotificacionOutboxRepository repository,
            final NotificacionChannelSender sender,
            final OutboxNotificationProperties properties,
            final MeterRegistry meterRegistry
    ) {
        return new NotificacionOutboxDispatcher(repository, sender, properties, meterRegistry, Clock.systemUTC());
    }

    @Bean
    @Conditional(OutboxAdapterCondition.class)
    @ConditionalOnProperty(
            prefix = "app.providers.outbox-notification",
            name = "sender",
            havingValue = "log",
            matchIfMissing = true
    )
    public NotificacionChannelSender logNotificacionChannelSender() {
        return new LogNotificacionChannelSender();
    }
}
