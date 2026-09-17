package co.edu.uco.asistenciasuco.infrastructure.config.adapters.vault.azure;

import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.vault.azure.AzureKeyVaultAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.AzureKeyVaultProviderProperties;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root para el proveedor Azure Key Vault.
 *
 * <p>Se activa unicamente cuando {@code app.adapters.vault.provider=azure_keyvault}.</p>
 * <p>Utiliza {@link DefaultAzureCredentialBuilder} respetando la politica institucional de Entra ID
 * (sin client secrets, autenticado mediante la sesion de Azure CLI en desarrollo).</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.adapters.vault",
        name = "provider",
        havingValue = "azure_keyvault",
        matchIfMissing = true
)
@EnableConfigurationProperties(AzureKeyVaultProviderProperties.class)
public class AzureKeyVaultAdapterConfiguration {

    @Bean
    public SecretVaultPort secretVaultPort(final AzureKeyVaultProviderProperties properties) {
        final SecretClient secretClient = new SecretClientBuilder()
                .vaultUrl(properties.endpoint())
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();

        return new AzureKeyVaultAdapter(secretClient);
    }
}
