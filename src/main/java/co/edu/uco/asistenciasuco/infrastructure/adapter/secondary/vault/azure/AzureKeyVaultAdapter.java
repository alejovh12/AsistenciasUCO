package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.vault.azure;

import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;

import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador secundario para Azure Key Vault.
 * Implementa {@link SecretVaultPort} utilizando el {@link SecretClient} oficial de Azure SDK.
 *
 * <p>No se autoregistra con anotaciones de Spring (@Component/@Service). Es instanciado
 * y configurado exclusivamente por el Composition Root.</p>
 */
public class AzureKeyVaultAdapter implements SecretVaultPort {

    private final SecretClient secretClient;

    public AzureKeyVaultAdapter(final SecretClient secretClient) {
        this.secretClient = Objects.requireNonNull(secretClient, "secretClient de Azure Key Vault es obligatorio.");
    }

    @Override
    public Optional<String> getSecret(final String secretName) {
        if (secretName == null || secretName.isBlank()) {
            return Optional.empty();
        }
        try {
            final KeyVaultSecret secret = secretClient.getSecret(secretName);
            if (secret == null || secret.getValue() == null) {
                return Optional.empty();
            }
            return Optional.of(secret.getValue());
        } catch (final ResourceNotFoundException e) {
            return Optional.empty();
        } catch (final Exception e) {
            throw new SecretVaultException("Error al consultar el secreto '" + secretName + "' en Azure Key Vault.", e);
        }
    }

    @Override
    public String getRequiredSecret(final String secretName) {
        return getSecret(secretName)
                .orElseThrow(() -> new SecretNotFoundException(secretName));
    }
}
