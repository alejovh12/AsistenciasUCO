package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.outbox;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.LogNotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionOutboxDispatcher;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionOutboxRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.OutboxNotificacionAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.condition.OutboxAdapterCondition;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.condition.OutboxSenderLogCondition;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters.NotificationAdapterProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.OutboxNotificationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
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
            final MeterRegistry meterRegistry
    ) {
        // Spring Boot 4 autoconfigura Jackson 3 (tools.jackson); este adaptador serializa con Jackson 2
        // y no depende de un bean del contenedor (igual que NotificacionOutboxDispatcher).
        return new OutboxNotificacionAdapter(repository, dispatcher, meterRegistry, new ObjectMapper());
    }

    @Bean
    @Conditional(OutboxAdapterCondition.class)
    public NotificacionOutboxDispatcher notificacionOutboxDispatcher(
            final NotificacionOutboxRepository repository,
            final ObjectProvider<NotificacionChannelSender> senderProvider,
            final OutboxNotificationProperties properties,
            final MeterRegistry meterRegistry
    ) {
        return new NotificacionOutboxDispatcher(repository, senderProvider.getObject(), properties, meterRegistry, Clock.systemUTC());
    }

    /** Canal de log: sender=log explicito o, sin sender, sin URL del Servicio de Notificaciones (pruebas y desarrollo aislado). */
    @Bean
    @Conditional({OutboxAdapterCondition.class, OutboxSenderLogCondition.class})
    public NotificacionChannelSender logNotificacionChannelSender() {
        return new LogNotificacionChannelSender();
    }
}
