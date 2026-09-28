package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuracion para la conexion con Azure Key Vault.
 */
@ConfigurationProperties(prefix = "app.providers.azure-keyvault")
public record AzureKeyVaultProviderProperties(
        String endpoint
) {
    public AzureKeyVaultProviderProperties {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("app.providers.azure-keyvault.endpoint es obligatorio.");
        }
    }
}
