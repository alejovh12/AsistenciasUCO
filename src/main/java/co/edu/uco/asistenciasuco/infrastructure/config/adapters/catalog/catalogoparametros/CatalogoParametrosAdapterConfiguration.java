package co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.catalogoparametros;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.catalogoparametros.CatalogoParametrosHttpParameterCatalogAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.composite.ParametroConValorPorDefectoConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.CatalogoParametrosProviderProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

/**
 * Composition Root del proveedor "Catalogo de Parametros" (componente externo por HTTP). Se activa unicamente con
 * {@code app.adapters.parameter-catalog.provider=catalogoparametros}; los consumidores reciben el decorador con valores
 * por defecto.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.adapters.parameter-catalog", name = "provider", havingValue = "catalogoparametros")
@EnableConfigurationProperties(CatalogoParametrosProviderProperties.class)
public class CatalogoParametrosAdapterConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogoParametrosAdapterConfiguration.class);

    @Bean(ParametroConValorPorDefectoConfiguration.DELEGADO)
    public ParameterCatalogPort parameterCatalogPort(final CatalogoParametrosProviderProperties properties) {
        LOGGER.info("Catalogo de parametros: proveedor catalogoparametros (baseUrl={}, tokenId={}, cacheTtl={}).",
                properties.baseUrl(), properties.tokenId(), properties.cacheTtl());
        final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        return new CatalogoParametrosHttpParameterCatalogAdapter(httpClient, properties);
    }
}
