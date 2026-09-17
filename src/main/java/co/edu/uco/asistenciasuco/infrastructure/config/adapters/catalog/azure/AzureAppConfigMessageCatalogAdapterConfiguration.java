package co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.azure;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.azure.AzureAppConfigMessageCatalogAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.AzureAppConfigProviderProperties;
import com.azure.data.appconfiguration.ConfigurationClient;
import com.azure.data.appconfiguration.ConfigurationClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root para el catálogo de mensajes en Azure App Configuration.
 *
 * <p>Se activa cuando {@code app.adapters.message-catalog.provider=azure}.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.adapters.message-catalog",
        name = "provider",
        havingValue = "azure",
        matchIfMissing = true
)
@EnableConfigurationProperties(AzureAppConfigProviderProperties.class)
public class AzureAppConfigMessageCatalogAdapterConfiguration {

    @Bean
    public MessageCatalogPort azureMessageCatalogPort(final AzureAppConfigProviderProperties properties) {
        final ConfigurationClient client = new ConfigurationClientBuilder()
                .endpoint(properties.endpoint())
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();

        return new AzureAppConfigMessageCatalogAdapter(client);
    }
}
