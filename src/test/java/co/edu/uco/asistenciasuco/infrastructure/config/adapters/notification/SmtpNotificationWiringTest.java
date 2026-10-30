package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp.CorreoTransport;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp.SmtpNotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp.TopeEnvios;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact.ContactoDestinatarioResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.LogNotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionOutboxRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaNotificacionRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaRenderer;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.composite.CompositeNotificacionAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.log.LogNotificacionAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.outbox.OutboxNotificacionAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.smtp.SmtpNotificacionAdapterConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * TEST_PLAN §3 (WI-S01..WI-S05): cableado de SmtpNotificacionAdapterConfiguration y fail-fast.
 */
class SmtpNotificationWiringTest {

    /**
     * WI-S05: El fixture solo provee los beans que el runtime real ya expone en infraestructura
     * (EntityManager, MeterRegistry y NotificacionOutboxRepository). No declara ObjectMapper ni otros beans.
     */
    @Configuration(proxyBeanMethods = false)
    static class RuntimeEquivalentInfraConfiguration {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        EntityManager entityManager() {
            return mock(EntityManager.class);
        }

        @Bean
        NotificacionOutboxRepository notificacionOutboxRepository() {
            return mock(NotificacionOutboxRepository.class);
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(
                    RuntimeEquivalentInfraConfiguration.class,
                    LogNotificacionAdapterConfiguration.class,
                    OutboxNotificacionAdapterConfiguration.class,
                    SmtpNotificacionAdapterConfiguration.class,
                    CompositeNotificacionAdapterConfiguration.class
            );

    @Test
    @DisplayName("WI-S01: sender=log (default): no se crean beans smtp; NotificacionChannelSender es LogNotificacionChannelSender")
    void WI_S01_sender_log_default_no_crea_beans_smtp() {
        runner.withPropertyValues(
                "app.adapters.notification.provider=outbox",
                "app.providers.outbox-notification.dispatcher-enabled=false"
        ).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(NotificacionChannelSender.class);
            assertThat(context.getBean(NotificacionChannelSender.class)).isInstanceOf(LogNotificacionChannelSender.class);
            assertThat(context).doesNotHaveBean(SmtpNotificacionChannelSender.class);
            assertThat(context).doesNotHaveBean(TopeEnvios.class);
            assertThat(context).doesNotHaveBean(PlantillaRenderer.class);
            assertThat(context).doesNotHaveBean(CorreoTransport.class);
        });
    }

    @Test
    @DisplayName("WI-S02 y WI-S05: sender=smtp con propiedades validas crea SmtpNotificacionChannelSender, TopeEnvios, renderizador y repositorios sin beans extra")
    void WI_S02_S05_sender_smtp_con_propiedades_validas_crea_beans() {
        runner.withPropertyValues(
                "app.adapters.notification.provider=outbox",
                "app.providers.outbox-notification.sender=smtp",
                "app.providers.outbox-notification.dispatcher-enabled=false",
                "app.providers.smtp-notification.host=smtp.uco.edu.co",
                "app.providers.smtp-notification.port=587",
                "app.providers.smtp-notification.from-address=notificaciones@uco.edu.co"
        ).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(NotificacionChannelSender.class);
            assertThat(context.getBean(NotificacionChannelSender.class)).isInstanceOf(SmtpNotificacionChannelSender.class);
            assertThat(context).hasSingleBean(TopeEnvios.class);
            assertThat(context).hasSingleBean(PlantillaRenderer.class);
            assertThat(context).hasSingleBean(PlantillaNotificacionRepository.class);
            assertThat(context).hasSingleBean(ContactoDestinatarioResolver.class);
            assertThat(context).hasSingleBean(CorreoTransport.class);
        });
    }

    @Test
    @DisplayName("WI-S03: sender=smtp con host o from-address vacios -> el contexto no arranca (fail-fast)")
    void WI_S03_sender_smtp_sin_host_o_from_address_falla_arranque() {
        runner.withPropertyValues(
                "app.adapters.notification.provider=outbox",
                "app.providers.outbox-notification.sender=smtp",
                "app.providers.outbox-notification.dispatcher-enabled=false",
                "app.providers.smtp-notification.host=",
                "app.providers.smtp-notification.from-address=notificaciones@uco.edu.co"
        ).run(context -> assertThat(context).hasFailed());

        runner.withPropertyValues(
                "app.adapters.notification.provider=outbox",
                "app.providers.outbox-notification.sender=smtp",
                "app.providers.outbox-notification.dispatcher-enabled=false",
                "app.providers.smtp-notification.host=smtp.uco.edu.co",
                "app.providers.smtp-notification.from-address="
        ).run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("WI-S04: sender=desconocido -> no arranca")
    void WI_S04_sender_desconocido_no_arranca() {
        runner.withPropertyValues(
                "app.adapters.notification.provider=outbox",
                "app.providers.outbox-notification.sender=desconocido"
        ).run(context -> assertThat(context).hasFailed());
    }
}
