package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuracion para la conexion con Azure Key Vault.
 *
 * <p>SEC-12: el endpoint no tiene valor por defecto en el repositorio. Con el proveedor Azure activo y sin
 * endpoint, el arranque falla con {@link #MENSAJE_ENDPOINT_OBLIGATORIO}.</p>
 */
@ConfigurationProperties(prefix = "app.providers.azure-keyvault")
public record AzureKeyVaultProviderProperties(
        String endpoint
) {
    public static final String MENSAJE_ENDPOINT_OBLIGATORIO =
            "app.providers.azure-keyvault.endpoint es obligatorio con app.adapters.vault.provider=azure_keyvault: "
                    + "defina AZURE_KEYVAULT_ENDPOINT, o use APP_ADAPTERS_VAULT_PROVIDER=local_env en local/test.";

    public AzureKeyVaultProviderProperties {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException(MENSAJE_ENDPOINT_OBLIGATORIO);
        }
    }
}
