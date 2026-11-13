package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuracion para la conexion con Azure App Configuration.
 *
 * <p>SEC-12: el endpoint no tiene valor por defecto en el repositorio. Con un proveedor de catalogo Azure activo
 * y sin endpoint, el arranque falla con {@link #MENSAJE_ENDPOINT_OBLIGATORIO}.</p>
 */
@ConfigurationProperties(prefix = "app.providers.azure-appconfig")
public record AzureAppConfigProviderProperties(
        String endpoint
) {
    public static final String MENSAJE_ENDPOINT_OBLIGATORIO =
            "app.providers.azure-appconfig.endpoint es obligatorio con app.adapters.parameter-catalog.provider="
                    + "azure_appconfig o app.adapters.message-catalog.provider=azure: defina AZURE_APPCONFIG_ENDPOINT, "
                    + "o use el proveedor sqlserver en local/test.";

    public AzureAppConfigProviderProperties {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException(MENSAJE_ENDPOINT_OBLIGATORIO);
        }
    }
}
