package co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.messageucolab;

import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarmensajes.usecase.SincronizarMensajesCatalogoUseCase;
import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarmensajes.usecase.impl.SincronizarMensajesCatalogoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.CatalogInvalidationPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogSourcePort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MensajeCatalogoRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.messaging.messageucolab.MessageUcoLabEventListener;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.messageucolab.MessageUcoLabCodeMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.messageucolab.MessageUcoLabMessageSourceAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.messageucolab.MessageUcoLabServiceIdentityTokenProvider;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.MessageUcoLabEventsProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.MessageUcoLabProviderProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.MessageUcoLabServiceIdentityProperties;
import co.edu.uco.messageucolab.client.api.MessageReader;
import co.edu.uco.messageucolab.client.http.MessageUcoLabAccessTokenProvider;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

/**
 * INT-003: composition root de MessageUcoLab 0.3.0 (solo con {@code app.adapters.message-catalog.provider=messageucolab}).
 *
 * <ul>
 *   <li>{@link MessageUcoLabAccessTokenProvider}: token de la identidad de servicio del backend. El starter 0.1.3 usa este
 *       bean en lugar del proveedor del starter de seguridad (su bean es {@code @ConditionalOnMissingBean}).</li>
 *   <li>Fuente del catalogo y caso de uso que copia los textos en la BD.</li>
 *   <li>Con {@code app.providers.messageucolab.events.enabled=true} (por defecto): cliente de Pulsar y suscripcion a los
 *       avisos del topic de la identidad.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.adapters.message-catalog", name = "provider", havingValue = "messageucolab")
@EnableConfigurationProperties({MessageUcoLabServiceIdentityProperties.class, MessageUcoLabEventsProperties.class})
public class MessageUcoLabSincronizacionConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageUcoLabSincronizacionConfiguration.class);

    @Bean
    public MessageUcoLabAccessTokenProvider messageUcoLabAccessTokenProvider(final MessageUcoLabServiceIdentityProperties identidad) {
        LOGGER.info("MessageUcoLab: identidad de servicio {}", identidad.clientId());
        return new MessageUcoLabServiceIdentityTokenProvider(
                identidad,
                HttpClient.newBuilder().connectTimeout(identidad.requestTimeout()).build(),
                JsonMapper.builder().build(),
                Clock.systemUTC());
    }

    @Bean
    public MessageCatalogSourcePort messageUcoLabMessageSourcePort(final MessageReader messageReader,
                                                                   final MessageUcoLabProviderProperties properties) {
        return new MessageUcoLabMessageSourceAdapter(
                messageReader,
                new MessageUcoLabCodeMapper(properties.codePrefix(), properties.codeAliases()),
                properties.timeout());
    }

    @Bean
    public SincronizarMensajesCatalogoUseCase sincronizarMensajesCatalogoUseCase(
            final MessageCatalogSourcePort fuente,
            final MensajeCatalogoRepositoryPort repositorio,
            final CatalogInvalidationPort invalidacion,
            final RealtimePublisherPort realtime
    ) {
        return new SincronizarMensajesCatalogoUseCaseImpl(fuente, repositorio, invalidacion, realtime);
    }

    /** Avisos de cambio por Pulsar (MessageUcoLab 0.3.0, paso 13 de la guia de actualizacion). */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "app.providers.messageucolab.events", name = "enabled", havingValue = "true",
            matchIfMissing = true)
    static class Eventos {

        @Bean(destroyMethod = "close")
        PulsarClient messageUcoLabPulsarClient(final MessageUcoLabEventsProperties eventos) throws PulsarClientException {
            return PulsarClient.builder()
                    .serviceUrl(eventos.serviceUrl())
                    .operationTimeout((int) Duration.ofSeconds(10).toSeconds(), java.util.concurrent.TimeUnit.SECONDS)
                    .build();
        }

        @Bean
        MessageUcoLabEventListener messageUcoLabEventListener(
                final PulsarClient messageUcoLabPulsarClient,
                final SincronizarMensajesCatalogoUseCase sincronizacion,
                final MessageUcoLabEventsProperties eventos,
                final MessageUcoLabServiceIdentityProperties identidad
        ) {
            return new MessageUcoLabEventListener(
                    messageUcoLabPulsarClient,
                    sincronizacion,
                    JsonMapper.builder().build(),
                    eventos.topicFor(identidad.clientId()),
                    eventos.subscriptionName(),
                    eventos.reconcileOnStartup());
        }
    }
}
