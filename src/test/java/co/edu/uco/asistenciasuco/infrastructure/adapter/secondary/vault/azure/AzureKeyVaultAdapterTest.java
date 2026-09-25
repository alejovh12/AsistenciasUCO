package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.vault.azure;

import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AzureKeyVaultAdapterTest {

    private SecretClient secretClient;
    private AzureKeyVaultAdapter adapter;

    @BeforeEach
    void setUp() {
        secretClient = Mockito.mock(SecretClient.class);
        adapter = new AzureKeyVaultAdapter(secretClient);
    }

    @Test
    void consulta_utiliza_cache_y_tras_invalidar_reconsulta_a_azure() {
        final KeyVaultSecret secret = Mockito.mock(KeyVaultSecret.class);
        when(secret.getValue()).thenReturn("top-secret-val");
        when(secretClient.getSecret(eq("jwt-signing-key"))).thenReturn(secret);

        final Optional<String> primero = adapter.getSecret("jwt-signing-key");
        assertTrue(primero.isPresent());
        assertEquals("top-secret-val", primero.get());
        verify(secretClient, times(1)).getSecret(eq("jwt-signing-key"));

        final Optional<String> segundo = adapter.getSecret("jwt-signing-key");
        assertTrue(segundo.isPresent());
        verify(secretClient, times(1)).getSecret(eq("jwt-signing-key"));

        adapter.invalidateSecret("jwt-signing-key");

        final Optional<String> tercero = adapter.getSecret("jwt-signing-key");
        assertTrue(tercero.isPresent());
        verify(secretClient, times(2)).getSecret(eq("jwt-signing-key"));
    }
}