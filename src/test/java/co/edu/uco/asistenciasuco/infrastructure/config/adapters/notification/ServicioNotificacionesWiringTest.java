package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.admin.AdministracionNotificacionPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.LogNotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionOutboxRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.service.AdministracionNotificacionServicioAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.service.NotificacionesServiceClient;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.service.ServicioNotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.outbox.OutboxNotificacionAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.servicio.NotificacionesServiceHealthIndicator;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.servicio.ServicioNotificacionesAdapterConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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

/** F4: seleccion del canal de salida del outbox y ensamblado del cliente, la administracion y la salud del servicio. */
class ServicioNotificacionesWiringTest {

    @Configuration(proxyBeanMethods = false)
    static class InfraSimulada {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        NotificacionOutboxRepository notificacionOutboxRepository() {
            return mock(NotificacionOutboxRepository.class);
        }

        @Bean
        EntityManager entityManager() {
            return mock(EntityManager.class);
        }

        @Bean
        CircuitBreakerRegistry circuitBreakerRegistry() {
            return CircuitBreakerRegistry.ofDefaults();
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(InfraSimulada.class, OutboxNotificacionAdapterConfiguration.class,
                    ServicioNotificacionesAdapterConfiguration.class)
            .withPropertyValues("app.adapters.notification.provider=outbox");

    @Test
    @DisplayName("SW-01: con URL y sin sender el canal es el servicio, con su salud y su administracion")
    void con_url_y_sin_sender_el_canal_es_el_servicio() {
        runner.withPropertyValues("asistencias.notificaciones.service.url=http://notificaciones:8080",
                "asistencias.notificaciones.service.api-key=clave").run(contexto -> {
            assertThat(contexto).hasSingleBean(NotificacionChannelSender.class);
            assertThat(contexto.getBean(NotificacionChannelSender.class)).isInstanceOf(ServicioNotificacionChannelSender.class);
            assertThat(contexto).hasSingleBean(NotificacionesServiceHealthIndicator.class);
            assertThat(contexto).hasSingleBean(NotificacionesServiceClient.class);
            assertThat(contexto.getBean(AdministracionNotificacionPort.class)).isInstanceOf(AdministracionNotificacionServicioAdapter.class);
        });
    }

    @Test
    @DisplayName("SW-02: sin URL ni sender el canal es el de log, sin salud del servicio, y la administracion responde 503")
    void sin_url_el_canal_es_log() {
        runner.run(contexto -> {
            assertThat(contexto.getBean(NotificacionChannelSender.class)).isInstanceOf(LogNotificacionChannelSender.class);
            assertThat(contexto).doesNotHaveBean(NotificacionesServiceHealthIndicator.class);
            assertThat(contexto).hasSingleBean(AdministracionNotificacionPort.class);
        });
    }

    @Test
    @DisplayName("SW-03: sender=log explicito gana sobre la URL")
    void sender_log_explicito_gana_sobre_la_url() {
        runner.withPropertyValues("asistencias.notificaciones.service.url=http://notificaciones:8080",
                "app.providers.outbox-notification.sender=log").run(contexto ->
                assertThat(contexto.getBean(NotificacionChannelSender.class)).isInstanceOf(LogNotificacionChannelSender.class));
    }

    @Test
    @DisplayName("SW-04: sender=servicio y el alias heredado smtp seleccionan el servicio")
    void servicio_y_el_alias_smtp_seleccionan_el_servicio() {
        for (final String sender : new String[] {"servicio", "smtp", "SERVICIO"}) {
            runner.withPropertyValues("asistencias.notificaciones.service.url=http://notificaciones:8080",
                    "app.providers.outbox-notification.sender=" + sender).run(contexto ->
                    assertThat(contexto.getBean(NotificacionChannelSender.class)).isInstanceOf(ServicioNotificacionChannelSender.class));
        }
    }

    @Test
    @DisplayName("SW-05: sender=servicio sin URL falla al arrancar con un mensaje que nombra la variable")
    void servicio_sin_url_falla_al_arrancar() {
        runner.withPropertyValues("app.providers.outbox-notification.sender=servicio").run(contexto -> {
            assertThat(contexto).hasFailed();
            assertThat(contexto.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
                    .rootCause().hasMessageContaining("NOTIFICACIONES_SERVICE_URL");
        });
    }

    @Test
    @DisplayName("SW-06: con provider=log no hay canal del outbox pero la administracion y el cliente siguen disponibles")
    void con_provider_log_no_hay_canal_de_outbox() {
        new ApplicationContextRunner()
                .withUserConfiguration(InfraSimulada.class, OutboxNotificacionAdapterConfiguration.class,
                        ServicioNotificacionesAdapterConfiguration.class)
                .withPropertyValues("asistencias.notificaciones.service.url=http://notificaciones:8080")
                .run(contexto -> {
                    assertThat(contexto).doesNotHaveBean(NotificacionChannelSender.class);
                    assertThat(contexto).hasSingleBean(AdministracionNotificacionPort.class);
                    assertThat(contexto).hasSingleBean(NotificacionesServiceClient.class);
                });
    }
}
