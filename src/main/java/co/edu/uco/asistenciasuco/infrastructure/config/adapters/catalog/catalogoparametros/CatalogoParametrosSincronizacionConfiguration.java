package co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.catalogoparametros;

import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarparametros.usecase.SincronizarParametrosCatalogoUseCase;
import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarparametros.usecase.impl.SincronizarParametrosCatalogoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.CatalogInvalidationPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogSourcePort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterValueRetentionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroCatalogoRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.messaging.catalogoparametros.CatalogoParametrosEventListener;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.CatalogoParametrosEventsProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.CatalogoParametrosProviderProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * INT-004: composition root de la sincronizacion de parametros del Catalogo de Parametros (solo con
 * {@code app.adapters.parameter-catalog.provider=catalogoparametros}).
 *
 * <ul>
 *   <li>Caso de uso que relee por M2M, invalida la cache y copia el valor en la BD (unico camino de escritura:
 *       {@code usp_sincronizar_parametro_catalogo_externo}).</li>
 *   <li>Con {@code app.providers.catalogo-parametros.events.enabled=true} (por defecto): cliente de Pulsar, suscripcion al topic de
 *       la identidad, reconciliacion de arranque (una sola lectura M2M) e indicador de salud.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.adapters.parameter-catalog", name = "provider", havingValue = "catalogoparametros")
@EnableConfigurationProperties({CatalogoParametrosProviderProperties.class, CatalogoParametrosEventsProperties.class})
public class CatalogoParametrosSincronizacionConfiguration {

    @Bean
    public SincronizarParametrosCatalogoUseCase sincronizarParametrosCatalogoUseCase(
            final ParameterCatalogSourcePort fuente,
            final ParametroCatalogoRepositoryPort repositorio,
            final CatalogInvalidationPort invalidacion,
            final ParameterValueRetentionPort retencion,
            final CatalogoParametrosProviderProperties properties,
            final ObjectProvider<Clock> clockProvider
    ) {
        return new SincronizarParametrosCatalogoUseCaseImpl(fuente, repositorio, invalidacion, properties.separador(),
                properties.applicationId(), properties.environmentId(), clockProvider.getIfAvailable(Clock::systemUTC), retencion);
    }

    /**
     * El escaneo de componentes registra esta clase anidada por si sola, sin pasar por la condicion de la externa: por eso repite
     * la condicion del provider. La condicion de los eventos va en cada bean.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "app.adapters.parameter-catalog", name = "provider", havingValue = "catalogoparametros")
    static class Eventos {

        @Bean(destroyMethod = "close")
        @ConditionalOnProperty(prefix = "app.providers.catalogo-parametros.events", name = "enabled", havingValue = "true",
                matchIfMissing = true)
        PulsarClient catalogoParametrosPulsarClient(final CatalogoParametrosEventsProperties eventos) throws PulsarClientException {
            return PulsarClient.builder()
                    .serviceUrl(eventos.serviceUrl())
                    .operationTimeout((int) Duration.ofSeconds(10).toSeconds(), TimeUnit.SECONDS)
                    .build();
        }

        @Bean
        @ConditionalOnProperty(prefix = "app.providers.catalogo-parametros.events", name = "enabled", havingValue = "true",
                matchIfMissing = true)
        CatalogoParametrosEventListener catalogoParametrosEventListener(
                final PulsarClient catalogoParametrosPulsarClient,
                final SincronizarParametrosCatalogoUseCase sincronizacion,
                final CatalogoParametrosEventsProperties eventos,
                final CatalogoParametrosProviderProperties proveedor,
                final MeterRegistry metricas
        ) {
            return new CatalogoParametrosEventListener(
                    catalogoParametrosPulsarClient,
                    sincronizacion,
                    JsonMapper.builder().build(),
                    eventos.topicFor(proveedor.clientId()),
                    eventos.subscriptionName(),
                    eventos.reconcileOnStartup(),
                    metricas);
        }

        @Bean
        @ConditionalOnProperty(prefix = "app.providers.catalogo-parametros.events", name = "enabled", havingValue = "true",
                matchIfMissing = true)
        CatalogoParametrosEventsHealthIndicator catalogoParametrosEventosHealthIndicator(
                final CatalogoParametrosEventListener listener) {
            return new CatalogoParametrosEventsHealthIndicator(listener);
        }
    }
}
