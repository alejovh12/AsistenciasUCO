package co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.catalogoparametros;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogSourcePort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.catalogoparametros.CatalogoParametrosHttpParameterCatalogAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.catalogoparametros.CatalogoParametrosM2mSourceAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.catalogoparametros.CatalogoParametrosM2mTokenProvider;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.catalogoparametros.CatalogoParametrosTokenProvider;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.composite.ParametroConValorPorDefectoConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.CatalogoParametrosProviderProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Clock;

/**
 * Composition Root del proveedor "Catalogo de Parametros" (componente externo por HTTP, M2M desde INT-004). Se activa unicamente con
 * {@code app.adapters.parameter-catalog.provider=catalogoparametros}; los consumidores reciben el decorador con valores
 * por defecto.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.adapters.parameter-catalog", name = "provider", havingValue = "catalogoparametros")
@EnableConfigurationProperties(CatalogoParametrosProviderProperties.class)
public class CatalogoParametrosAdapterConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogoParametrosAdapterConfiguration.class);

    /** INT-004: token {@code client_credentials} de la identidad de servicio; lo comparten el proveedor con cache y la fuente M2M. */
    @Bean
    public CatalogoParametrosTokenProvider catalogoParametrosTokenProvider(final CatalogoParametrosProviderProperties properties,
                                                                           final ObjectProvider<Clock> clockProvider) {
        LOGGER.info("Catalogo de parametros: identidad de servicio M2M {} (token-uri={}).", properties.clientId(), properties.tokenUri());
        return new CatalogoParametrosM2mTokenProvider(properties, httpClient(properties), JsonMapper.builder().build(),
                clockProvider.getIfAvailable(Clock::systemUTC));
    }

    /** Tambien es el unico {@code ParameterValueRetentionPort}: retiene el ultimo valor valido si la BD rechaza el del Catalogo. */
    @Bean(ParametroConValorPorDefectoConfiguration.DELEGADO)
    public CatalogoParametrosHttpParameterCatalogAdapter parameterCatalogPort(final CatalogoParametrosProviderProperties properties,
                                                     final CatalogoParametrosTokenProvider tokenProvider,
                                                     final ObjectProvider<Clock> clockProvider) {
        LOGGER.info("Catalogo de parametros: proveedor catalogoparametros por M2M (baseUrl={}, cacheTtl={}).",
                properties.baseUrl(), properties.cacheTtl());
        return new CatalogoParametrosHttpParameterCatalogAdapter(httpClient(properties), properties, tokenProvider,
                clockProvider.getIfAvailable(Clock::systemUTC));
    }

    /** INT-004: lectura puntual (sin cache) para confirmar el valor despues de un evento y para la reconciliacion. */
    @Bean
    public ParameterCatalogSourcePort catalogoParametrosSourcePort(final CatalogoParametrosProviderProperties properties,
                                                                   final CatalogoParametrosTokenProvider tokenProvider) {
        return new CatalogoParametrosM2mSourceAdapter(httpClient(properties), properties, tokenProvider);
    }

    private static HttpClient httpClient(final CatalogoParametrosProviderProperties properties) {
        return HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
    }
}
