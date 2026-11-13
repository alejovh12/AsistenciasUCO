package co.edu.uco.asistenciasuco.infrastructure.config.adapters.vault.azure;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.vault.azure.AzureKeyVaultAdapter;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;

/**
 * Fabrica unica del cliente de Azure Key Vault (Composition Root).
 *
 * <p>La usan el bean {@code SecretVaultPort} ({@link AzureKeyVaultAdapterConfiguration}) y la carga temprana de
 * secretos de ejecucion (VAL-oleada3-baul-secretos), para que ambos caminos autentiquen igual: siempre con
 * {@code DefaultAzureCredential}, sin client secrets. Construir el cliente no abre conexiones.</p>
 */
public final class AzureKeyVaultClientFactory {

    private AzureKeyVaultClientFactory() {
    }

    public static SecretClient crearCliente(final String endpoint) {
        return new SecretClientBuilder()
                .vaultUrl(endpoint)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }

    public static AzureKeyVaultAdapter crearAdaptador(final String endpoint) {
        return new AzureKeyVaultAdapter(crearCliente(endpoint));
    }
}
