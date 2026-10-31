package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification;

import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.usecase.impl.RetirarEstudianteGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.RetiroEstudianteGrupoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.LogNotificacionAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.composite.CompositeNotificacionAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionOutboxDispatcher;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionOutboxRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.OutboxNotificacionAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.composite.CompositeNotificacionAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.log.LogNotificacionAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.outbox.OutboxNotificacionAdapterConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class NotificationWiringTest {

    @Configuration(proxyBeanMethods = false)
    static class MockInfraConfiguration {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        NotificacionOutboxRepository notificacionOutboxRepository() {
            return mock(NotificacionOutboxRepository.class);
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(
                    MockInfraConfiguration.class,
                    LogNotificacionAdapterConfiguration.class,
                    OutboxNotificacionAdapterConfiguration.class,
                    CompositeNotificacionAdapterConfiguration.class
            );

    @Test
    @DisplayName("WI-01: sin propiedades crea 1 NotificacionPort de tipo LogNotificacionAdapter")
    void WI01_sin_propiedades_usa_log_por_defecto() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(NotificacionPort.class);
            assertThat(context.getBean(NotificacionPort.class)).isInstanceOf(LogNotificacionAdapter.class);
            assertThat(context).doesNotHaveBean(NotificacionOutboxDispatcher.class);
        });
    }

    @Test
    @DisplayName("WI-02: provider=log crea 1 NotificacionPort de tipo LogNotificacionAdapter")
    void WI02_provider_log() {
        runner.withPropertyValues("app.adapters.notification.provider=log")
                .run(context -> {
                    assertThat(context).hasSingleBean(NotificacionPort.class);
                    assertThat(context.getBean(NotificacionPort.class)).isInstanceOf(LogNotificacionAdapter.class);
                    assertThat(context).doesNotHaveBean(NotificacionOutboxDispatcher.class);
                });
    }

    @Test
    @DisplayName("WI-03: provider=outbox crea 1 NotificacionPort de tipo OutboxNotificacionAdapter")
    void WI03_provider_outbox() {
        runner.withPropertyValues("app.adapters.notification.provider=outbox")
                .run(context -> {
                    assertThat(context).hasSingleBean(NotificacionPort.class);
                    assertThat(context.getBean(NotificacionPort.class)).isInstanceOf(OutboxNotificacionAdapter.class);
                    assertThat(context).hasSingleBean(NotificacionOutboxDispatcher.class);
                    assertThat(context).hasSingleBean(NotificacionChannelSender.class);
                });
    }

    private void assertCompositeNotificacionPort(final AssertableApplicationContext context) {
        assertDoesNotThrow(() -> {
            final NotificacionPort port = context.getBeanProvider(NotificacionPort.class).getObject();
            assertThat(port).isInstanceOf(CompositeNotificacionAdapter.class);
        });
        final String[] portBeanNames = context.getBeanNamesForType(NotificacionPort.class);
        final List<String> primaryBeans = new ArrayList<>();
        for (final String beanName : portBeanNames) {
            if (context.getBeanFactory().getBeanDefinition(beanName).isPrimary()) {
                primaryBeans.add(beanName);
            }
        }
        assertThat(primaryBeans).hasSize(1);
    }

    @Test
    @DisplayName("WI-04: provider=composite targets=log,outbox crea Composite con 2 destinos en orden")
    void WI04_provider_composite_log_outbox() {
        runner.withPropertyValues(
                "app.adapters.notification.provider=composite",
                "app.adapters.notification.composite.targets=log,outbox"
        ).run(context -> {
            assertCompositeNotificacionPort(context);
            assertThat(context).hasSingleBean(LogNotificacionAdapter.class);
            assertThat(context).hasSingleBean(OutboxNotificacionAdapter.class);
            assertThat(context).hasSingleBean(NotificacionOutboxDispatcher.class);
        });
    }

    @Test
    @DisplayName("WI-05: provider=composite targets=outbox,log respeta orden inverso")
    void WI05_provider_composite_outbox_log() {
        runner.withPropertyValues(
                "app.adapters.notification.provider=composite",
                "app.adapters.notification.composite.targets=outbox,log"
        ).run(context -> {
            assertCompositeNotificacionPort(context);
            assertThat(context).hasSingleBean(LogNotificacionAdapter.class);
            assertThat(context).hasSingleBean(OutboxNotificacionAdapter.class);
        });
    }

    @Test
    @DisplayName("WI-06: composite invalido (1 destino / repetidos) falla arranque")
    void WI06_composite_invalido_falla() {
        runner.withPropertyValues(
                "app.adapters.notification.provider=composite",
                "app.adapters.notification.composite.targets=log"
        ).run(context -> {
            assertThat(context).hasFailed();
        });

        runner.withPropertyValues(
                "app.adapters.notification.provider=composite",
                "app.adapters.notification.composite.targets=log,log"
        ).run(context -> {
            assertThat(context).hasFailed();
        });
    }

    @Test
    @DisplayName("WI-07: provider desconocido falla el arranque")
    void WI07_provider_desconocido_falla() {
        runner.withPropertyValues("app.adapters.notification.provider=kafka")
                .run(context -> {
                    assertThat(context).hasFailed();
                });
    }

    @Test
    @DisplayName("WI-08: sender desconocido falla el arranque")
    void WI08_sender_desconocido_falla() {
        runner.withPropertyValues(
                "app.adapters.notification.provider=outbox",
                "app.providers.outbox-notification.sender=desconocido"
        ).run(context -> {
            assertThat(context).hasFailed();
        });
    }

    @Test
    @DisplayName("WI-09: provider=outbox con dispatcher-enabled=false no inicia hilo")
    void WI09_dispatcher_deshabilitado() {
        runner.withPropertyValues(
                "app.adapters.notification.provider=outbox",
                "app.providers.outbox-notification.dispatcher-enabled=false"
        ).run(context -> {
            assertThat(context).hasSingleBean(NotificacionPort.class);
            assertThat(context).hasSingleBean(NotificacionOutboxDispatcher.class);
            final NotificacionOutboxDispatcher disp = context.getBean(NotificacionOutboxDispatcher.class);
            assertFalse(disp.isRunning());
        });
    }

    @Test
    @DisplayName("WI-10: provider=log no crea beans del paquete outbox")
    void WI10_provider_log_sin_beans_outbox() {
        runner.withPropertyValues("app.adapters.notification.provider=log")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OutboxNotificacionAdapter.class);
                    assertThat(context).doesNotHaveBean(NotificacionOutboxDispatcher.class);
                    assertThat(context).doesNotHaveBean(NotificacionChannelSender.class);
                });
    }

    @Test
    @DisplayName("WI-11: ningun caso crea dos beans NotificacionPort")
    void WI11_exactamente_un_notificacion_port() {
        runner.withPropertyValues("app.adapters.notification.provider=log")
                .run(context -> assertThat(context).hasSingleBean(NotificacionPort.class));
        runner.withPropertyValues("app.adapters.notification.provider=outbox")
                .run(context -> assertThat(context).hasSingleBean(NotificacionPort.class));
        runner.withPropertyValues(
                "app.adapters.notification.provider=composite",
                "app.adapters.notification.composite.targets=log,outbox"
        ).run(this::assertCompositeNotificacionPort);
    }

    @Test
    @DisplayName("WI-12: RetirarEstudianteGrupoUseCaseImpl construible con el NotificacionPort en cada modo")
    void WI12_use_case_construible() {
        runner.withPropertyValues("app.adapters.notification.provider=log")
                .run(context -> {
                    final NotificacionPort port = context.getBean(NotificacionPort.class);
                    assertDoesNotThrow(() -> new RetirarEstudianteGrupoUseCaseImpl(
                            mock(InstitutionalScopePort.class),
                            mock(RetiroEstudianteGrupoPort.class),
                            port
                    ));
                });

        runner.withPropertyValues("app.adapters.notification.provider=outbox")
                .run(context -> {
                    final NotificacionPort port = context.getBean(NotificacionPort.class);
                    assertDoesNotThrow(() -> new RetirarEstudianteGrupoUseCaseImpl(
                            mock(InstitutionalScopePort.class),
                            mock(RetiroEstudianteGrupoPort.class),
                            port
                    ));
                });
    }
}
