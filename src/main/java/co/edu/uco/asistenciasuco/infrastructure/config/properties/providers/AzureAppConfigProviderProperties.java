package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuracion para la conexion con Azure App Configuration.
 */
@ConfigurationProperties(prefix = "app.providers.azure-appconfig")
public record AzureAppConfigProviderProperties(
        String endpoint
) {
    public AzureAppConfigProviderProperties {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("app.providers.azure-appconfig.endpoint es obligatorio.");
        }
    }
}
