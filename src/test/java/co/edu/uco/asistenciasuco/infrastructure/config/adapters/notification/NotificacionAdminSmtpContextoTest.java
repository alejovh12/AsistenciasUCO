package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.admin.AdministracionNotificacionPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp.SmtpNotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.LogNotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionOutboxRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaNotificacionRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaPorDefectoCatalogo;
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
 * QUALITY-SONAR-RELIABILITY (java:S9352), prueba de contexto Spring sin correo real: Admin y Smtp declaran beans del mismo
 * tipo, y segun el orden de registro pueden coexistir ambos. Con ambos presentes la inyeccion debe resolverse (un solo
 * {@code @Primary}) y el proveedor del canal no cambia: SMTP solo con sender=smtp, Log en cualquier otro caso.
 */
class NotificacionAdminSmtpContextoTest {

    @Configuration(proxyBeanMethods = false)
    static class InfraEquivalente {
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

    private static final String[] SMTP = {
            "app.adapters.notification.provider=outbox",
            "app.providers.outbox-notification.sender=smtp",
            "app.providers.outbox-notification.dispatcher-enabled=false",
            "app.providers.smtp-notification.host=smtp.uco.edu.co",
            "app.providers.smtp-notification.port=587",
            "app.providers.smtp-notification.from-address=notificaciones@uco.edu.co"
    };

    private static final String[] LOG = {
            "app.adapters.notification.provider=outbox",
            "app.providers.outbox-notification.dispatcher-enabled=false"
    };

    private static ApplicationContextRunner runner(final boolean adminPrimero) {
        final ApplicationContextRunner base = new ApplicationContextRunner().withUserConfiguration(
                InfraEquivalente.class,
                LogNotificacionAdapterConfiguration.class,
                OutboxNotificacionAdapterConfiguration.class);
        final ApplicationContextRunner conOrden = adminPrimero
                ? base.withUserConfiguration(AdminNotificacionAdapterConfiguration.class,
                        SmtpNotificacionAdapterConfiguration.class)
                : base.withUserConfiguration(SmtpNotificacionAdapterConfiguration.class,
                        AdminNotificacionAdapterConfiguration.class);
        return conOrden.withUserConfiguration(CompositeNotificacionAdapterConfiguration.class);
    }

    private static void sinAmbiguedad(final org.springframework.context.ApplicationContext context) {
        assertThat(context.getBean(PlantillaNotificacionRepository.class)).isNotNull();
        assertThat(context.getBean(PlantillaPorDefectoCatalogo.class)).isNotNull();
        assertThat(context.getBean(PlantillaRenderer.class)).isNotNull();
        assertThat(context.getBean(AdministracionNotificacionPort.class)).isNotNull();
        assertThat(context.getBean(NotificacionPort.class)).isNotNull();
    }

    @Test
    @DisplayName("sender=smtp, Admin antes que Smtp: contexto arranca, resuelve las plantillas y el canal es SMTP")
    void smtp_con_admin_primero() {
        runner(true).withPropertyValues(SMTP).run(context -> {
            assertThat(context).hasNotFailed();
            sinAmbiguedad(context);
            assertThat(context.getBean(NotificacionChannelSender.class)).isInstanceOf(SmtpNotificacionChannelSender.class);
        });
    }

    @Test
    @DisplayName("sender=smtp, Smtp antes que Admin (coexisten ambos beans): contexto arranca y el canal es SMTP")
    void smtp_con_smtp_primero_donde_coexisten_ambos_beans() {
        runner(false).withPropertyValues(SMTP).run(context -> {
            assertThat(context).hasNotFailed();
            sinAmbiguedad(context);
            assertThat(context.getBean(NotificacionChannelSender.class)).isInstanceOf(SmtpNotificacionChannelSender.class);
        });
    }

    @Test
    @DisplayName("sender=log (por defecto): solo existen los beans Admin y el canal sigue siendo Log, en ambos ordenes")
    void log_no_activa_smtp_en_ningun_orden() {
        for (final boolean adminPrimero : new boolean[] {true, false}) {
            runner(adminPrimero).withPropertyValues(LOG).run(context -> {
                assertThat(context).hasNotFailed();
                sinAmbiguedad(context);
                assertThat(context.getBean(NotificacionChannelSender.class)).isInstanceOf(LogNotificacionChannelSender.class);
                assertThat(context).doesNotHaveBean(SmtpNotificacionChannelSender.class);
            });
        }
    }
}
