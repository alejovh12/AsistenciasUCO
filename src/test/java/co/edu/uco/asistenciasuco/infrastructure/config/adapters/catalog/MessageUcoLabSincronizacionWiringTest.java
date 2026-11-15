package co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog;

import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarmensajes.usecase.SincronizarMensajesCatalogoUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.CatalogInvalidationPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogSourcePort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MensajeCatalogoRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.messaging.messageucolab.MessageUcoLabEventListener;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.messageucolab.MessageUcoLabServiceIdentityTokenProvider;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.messageucolab.MessageUcoLabMessageCatalogAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.messageucolab.MessageUcoLabSincronizacionConfiguration;
import co.edu.uco.messageucolab.client.autoconfigure.MessageUcoLabAutoConfiguration;
import co.edu.uco.messageucolab.client.http.MessageUcoLabAccessTokenProvider;
import org.apache.pulsar.client.api.PulsarClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration;
import org.springframework.core.NestedExceptionUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * INT-003: con MessageUcoLab 0.3.0 el starter usa el token de la identidad de servicio del backend (no el del starter de
 * seguridad), y los avisos de Pulsar se activan o no por propiedad. Con otro provider no se registra nada.
 */
class MessageUcoLabSincronizacionWiringTest {

    private static final String[] BASE = {
            "messageucolab.url=http://localhost:8000",
            "app.adapters.message-catalog.provider=messageucolab",
            "app.providers.messageucolab.fallback=none",
            "app.providers.messageucolab.service-identity.token-uri=http://localhost:19090/realms/r/protocol/openid-connect/token",
            "app.providers.messageucolab.service-identity.client-id=svc-1-asistencias-dev",
            "app.providers.messageucolab.service-identity.client-secret=valor-secreto-de-prueba"
    };

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebClientAutoConfiguration.class, MessageUcoLabAutoConfiguration.class))
            .withUserConfiguration(MessageUcoLabMessageCatalogAdapterConfiguration.class, MessageUcoLabSincronizacionConfiguration.class)
            .withBean(MensajeCatalogoRepositoryPort.class, () -> mock(MensajeCatalogoRepositoryPort.class))
            .withBean(CatalogInvalidationPort.class, () -> mock(CatalogInvalidationPort.class))
            .withBean(RealtimePublisherPort.class, () -> mock(RealtimePublisherPort.class));

    @Test
    void el_starter_usa_el_token_de_la_identidad_del_backend() {
        runner.withPropertyValues(BASE).withPropertyValues("app.providers.messageucolab.events.enabled=false")
                .run(context -> {
                    assertEquals(1, context.getBeansOfType(MessageUcoLabAccessTokenProvider.class).size());
                    assertInstanceOf(MessageUcoLabServiceIdentityTokenProvider.class, context.getBean(MessageUcoLabAccessTokenProvider.class));
                    assertNotNull(context.getBean(MessageCatalogSourcePort.class));
                    assertNotNull(context.getBean(SincronizarMensajesCatalogoUseCase.class));
                    assertTrue(context.getBeansOfType(MessageUcoLabEventListener.class).isEmpty());
                    assertTrue(context.getBeansOfType(PulsarClient.class).isEmpty());
                });
    }

    @Test
    void con_avisos_activos_registra_cliente_de_pulsar_y_listener_aunque_no_haya_broker() {
        runner.withPropertyValues(BASE).withPropertyValues(
                        "app.providers.messageucolab.events.service-url=pulsar://127.0.0.1:1",
                        "app.providers.messageucolab.events.reconcile-on-startup=false")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertNotNull(context.getBean(PulsarClient.class));
                    assertTrue(context.getBean(MessageUcoLabEventListener.class).isRunning());
                });
    }

    @Test
    void sin_secreto_de_la_identidad_falla_con_mensaje_claro() {
        runner.withPropertyValues(BASE).withPropertyValues(
                        "app.providers.messageucolab.service-identity.client-secret=",
                        "app.providers.messageucolab.events.enabled=false")
                .run(context -> {
                    assertNotNull(context.getStartupFailure());
                    assertTrue(NestedExceptionUtils.getMostSpecificCause(context.getStartupFailure()).getMessage()
                            .contains("client-secret"));
                });
    }

    /**
     * Regresion del Backend Integration Gate del PR #150: el escaneo de componentes encuentra la clase anidada
     * {@code Eventos} por si sola. Con otro provider el contexto completo fallaba al pedir
     * {@code MessageUcoLabEventsProperties}, que solo registra la clase externa.
     */
    @Test
    void con_otro_provider_la_configuracion_de_avisos_escaneada_sola_no_registra_nada() {
        new ApplicationContextRunner()
                .withUserConfiguration(eventosAnidada())
                .withPropertyValues("app.adapters.message-catalog.provider=sqlserver")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertTrue(context.getBeansOfType(PulsarClient.class).isEmpty());
                    assertTrue(context.getBeansOfType(MessageUcoLabEventListener.class).isEmpty());
                });
    }

    @Test
    void sin_provider_la_configuracion_de_avisos_escaneada_sola_no_registra_nada() {
        new ApplicationContextRunner()
                .withUserConfiguration(eventosAnidada())
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertTrue(context.getBeansOfType(PulsarClient.class).isEmpty());
                });
    }

    private static Class<?> eventosAnidada() {
        return java.util.Arrays.stream(MessageUcoLabSincronizacionConfiguration.class.getDeclaredClasses())
                .filter(c -> c.getSimpleName().equals("Eventos"))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void con_otro_provider_no_registra_nada_de_messageucolab() {
        runner.withPropertyValues("app.adapters.message-catalog.provider=sqlserver", "messageucolab.url=http://localhost:8000")
                .run(context -> {
                    assertTrue(context.getBeansOfType(MessageCatalogSourcePort.class).isEmpty());
                    assertTrue(context.getBeansOfType(SincronizarMensajesCatalogoUseCase.class).isEmpty());
                    assertTrue(context.getBeansOfType(MessageUcoLabServiceIdentityTokenProvider.class).isEmpty());
                });
    }
}
