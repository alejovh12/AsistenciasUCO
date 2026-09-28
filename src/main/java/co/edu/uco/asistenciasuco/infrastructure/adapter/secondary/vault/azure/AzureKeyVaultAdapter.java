package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.vault.azure;

import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort;
import com.azure.core.exception.HttpResponseException;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador secundario para Azure Key Vault.
 * Implementa {@link SecretVaultPort} utilizando el {@link SecretClient} oficial de Azure SDK.
 *
 * <p>Incorpora una caché acotada en memoria (Caffeine: 50 elementos, TTL 5 minutos)
 * con capacidad de invalidación inmediata (Push) y evicción por fallo ante excepciones 401.</p>
 *
 * <p>No se autoregistra con anotaciones de Spring (@Component/@Service). Es instanciado
 * y configurado exclusivamente por el Composition Root.</p>
 */
public class AzureKeyVaultAdapter implements SecretVaultPort {

    private final SecretClient secretClient;
    private final Cache<String, String> cache;

    public AzureKeyVaultAdapter(final SecretClient secretClient) {
        this(secretClient, Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterWrite(Duration.ofMinutes(5))
                .build());
    }

    public AzureKeyVaultAdapter(final SecretClient secretClient, final Cache<String, String> cache) {
        this.secretClient = Objects.requireNonNull(secretClient, "secretClient de Azure Key Vault es obligatorio.");
        this.cache = Objects.requireNonNull(cache, "Cache de Caffeine es obligatoria.");
    }

    @Override
    public Optional<String> getSecret(final String secretName) {
        if (secretName == null || secretName.isBlank()) {
            return Optional.empty();
        }

        final String cleanName = secretName.trim();
        final String cached = cache.getIfPresent(cleanName);
        if (cached != null) {
            return Optional.of(cached);
        }

        try {
            final KeyVaultSecret secret = secretClient.getSecret(cleanName);
            if (secret == null || secret.getValue() == null) {
                return Optional.empty();
            }
            cache.put(cleanName, secret.getValue());
            return Optional.of(secret.getValue());
        } catch (final ResourceNotFoundException e) {
            return Optional.empty();
        } catch (final HttpResponseException e) {
            // Nivel 2: Si hay error de autenticación/autorización (401/403) evictar inmediatamente
            if (e.getResponse() != null && (e.getResponse().getStatusCode() == 401 || e.getResponse().getStatusCode() == 403)) {
                cache.invalidate(cleanName);
            }
            throw new SecretVaultException("Error de respuesta al consultar el secreto '" + cleanName + "' en Azure Key Vault.", e);
        } catch (final Exception e) {
            throw new SecretVaultException("Error al consultar el secreto '" + cleanName + "' en Azure Key Vault.", e);
        }
    }

    @Override
    public String getRequiredSecret(final String secretName) {
        return getSecret(secretName)
                .orElseThrow(() -> new SecretNotFoundException(secretName));
    }

    /**
     * Invalida un secreto específico de la caché en memoria.
     */
    public void invalidateSecret(final String secretName) {
        if (secretName != null && !secretName.isBlank()) {
            cache.invalidate(secretName.trim());
        }
    }

    /**
     * Invalida todos los secretos cacheados en memoria.
     */
    public void invalidateAll() {
        cache.invalidateAll();
    }
}
