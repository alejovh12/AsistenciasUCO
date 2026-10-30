package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.smtp;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp.CorreoTransport;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp.JakartaMailCorreoTransport;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp.SmtpNotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp.TopeEnvios;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact.ContactoDestinatarioJpaRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact.ContactoDestinatarioResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaNotificacionJpaRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaNotificacionRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaPorDefectoCatalogo;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaRenderer;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.condition.OutboxAdapterCondition;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SmtpNotificationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Composition Root del canal SMTP sobre el proveedor outbox de notificaciones (CONTRACT §4.5).
 * Solo se activa cuando {@code outbox} esta activo y {@code app.providers.outbox-notification.sender=smtp}.
 */
@Configuration(proxyBeanMethods = false)
@Conditional(OutboxAdapterCondition.class)
@ConditionalOnProperty(
        prefix = "app.providers.outbox-notification",
        name = "sender",
        havingValue = "smtp"
)
@EnableConfigurationProperties(SmtpNotificationProperties.class)
public class SmtpNotificacionAdapterConfiguration {

    @Bean
    @ConditionalOnMissingBean(PlantillaNotificacionRepository.class)
    public PlantillaNotificacionRepository plantillaNotificacionRepository(
            final EntityManager entityManager,
            final SmtpNotificationProperties properties
    ) {
        return new PlantillaNotificacionJpaRepository(
                entityManager,
                Clock.systemUTC(),
                properties.templateCacheTtl()
        );
    }

    @Bean
    @ConditionalOnMissingBean(PlantillaPorDefectoCatalogo.class)
    public PlantillaPorDefectoCatalogo plantillaPorDefectoCatalogo() {
        return new PlantillaPorDefectoCatalogo();
    }

    @Bean
    @ConditionalOnMissingBean(PlantillaRenderer.class)
    public PlantillaRenderer plantillaRenderer(
            final PlantillaNotificacionRepository plantillaRepository,
            final PlantillaPorDefectoCatalogo catalogoPorDefecto,
            final MeterRegistry meterRegistry,
            final SmtpNotificationProperties properties
    ) {
        return new PlantillaRenderer(
                plantillaRepository,
                catalogoPorDefecto,
                meterRegistry,
                properties.platformUrl()
        );
    }

    @Bean
    @ConditionalOnMissingBean(ContactoDestinatarioResolver.class)
    public ContactoDestinatarioResolver contactoDestinatarioResolver(final EntityManager entityManager) {
        return new ContactoDestinatarioJpaRepository(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean(TopeEnvios.class)
    public TopeEnvios topeEnvios(
            final SmtpNotificationProperties properties,
            final MeterRegistry meterRegistry
    ) {
        return new TopeEnvios(
                properties.ratePerMinute(),
                properties.ratePerDay(),
                Clock.systemUTC(),
                meterRegistry
        );
    }

    @Bean
    @ConditionalOnMissingBean(CorreoTransport.class)
    public CorreoTransport correoTransport(final SmtpNotificationProperties properties) {
        return new JakartaMailCorreoTransport(properties);
    }

    @Bean
    public NotificacionChannelSender smtpNotificacionChannelSender(
            final ContactoDestinatarioResolver contactoResolver,
            final PlantillaRenderer plantillaRenderer,
            final CorreoTransport correoTransport,
            final TopeEnvios topeEnvios,
            final SmtpNotificationProperties properties,
            final MeterRegistry meterRegistry
    ) {
        return new SmtpNotificacionChannelSender(
                contactoResolver,
                plantillaRenderer,
                correoTransport,
                topeEnvios,
                properties,
                meterRegistry
        );
    }
}
