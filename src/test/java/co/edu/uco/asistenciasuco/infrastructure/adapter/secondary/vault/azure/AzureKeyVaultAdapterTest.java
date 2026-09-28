package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.vault.azure;

import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort.SecretNotFoundException;
import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort.SecretVaultException;
import com.azure.core.exception.HttpResponseException;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.core.http.HttpResponse;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AzureKeyVaultAdapterTest {

    private SecretClient secretClient;
    private Cache<String, String> cache;
    private AzureKeyVaultAdapter adapter;

    @BeforeEach
    void setUp() {
        secretClient = Mockito.mock(SecretClient.class);
        cache = Caffeine.newBuilder().build();
        adapter = new AzureKeyVaultAdapter(secretClient, cache);
    }

    private static KeyVaultSecret secretWithValue(final String value) {
        final KeyVaultSecret secret = Mockito.mock(KeyVaultSecret.class);
        when(secret.getValue()).thenReturn(value);
        return secret;
    }

    private static HttpResponse responseWithStatus(final int status) {
        final HttpResponse response = Mockito.mock(HttpResponse.class);
        when(response.getStatusCode()).thenReturn(status);
        return response;
    }

    @Test
    void consulta_utiliza_cache_y_tras_invalidar_reconsulta_a_azure() {
        final AzureKeyVaultAdapter defaultCacheAdapter = new AzureKeyVaultAdapter(secretClient);
        final KeyVaultSecret secret = secretWithValue("top-secret-val");
        when(secretClient.getSecret(eq("jwt-signing-key"))).thenReturn(secret);

        final Optional<String> primero = defaultCacheAdapter.getSecret("jwt-signing-key");
        assertTrue(primero.isPresent());
        assertEquals("top-secret-val", primero.get());
        verify(secretClient, times(1)).getSecret(eq("jwt-signing-key"));

        final Optional<String> segundo = defaultCacheAdapter.getSecret("jwt-signing-key");
        assertTrue(segundo.isPresent());
        verify(secretClient, times(1)).getSecret(eq("jwt-signing-key"));

        defaultCacheAdapter.invalidateSecret("jwt-signing-key");

        final Optional<String> tercero = defaultCacheAdapter.getSecret("jwt-signing-key");
        assertTrue(tercero.isPresent());
        verify(secretClient, times(2)).getSecret(eq("jwt-signing-key"));
    }

    @Test
    void constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new AzureKeyVaultAdapter(null));
        assertThrows(NullPointerException.class, () -> new AzureKeyVaultAdapter(secretClient, null));
    }

    @Test
    void getSecret_con_nombre_nulo_o_en_blanco_devuelve_vacio_sin_consultar_azure() {
        assertTrue(adapter.getSecret(null).isEmpty());
        assertTrue(adapter.getSecret("").isEmpty());
        assertTrue(adapter.getSecret("   ").isEmpty());

        verifyNoInteractions(secretClient);
    }

    @Test
    void getSecret_normaliza_el_nombre_con_trim_antes_de_consultar_y_cachear() {
        final KeyVaultSecret secret = secretWithValue("valor");
        when(secretClient.getSecret("db-password")).thenReturn(secret);

        assertEquals(Optional.of("valor"), adapter.getSecret("  db-password  "));
        assertEquals("valor", cache.getIfPresent("db-password"));
    }

    @Test
    void getSecret_devuelve_vacio_cuando_azure_devuelve_secreto_nulo() {
        when(secretClient.getSecret("missing")).thenReturn(null);

        assertTrue(adapter.getSecret("missing").isEmpty());
        assertNull(cache.getIfPresent("missing"));
    }

    @Test
    void getSecret_devuelve_vacio_cuando_el_secreto_no_tiene_valor() {
        final KeyVaultSecret secret = secretWithValue(null);
        when(secretClient.getSecret("empty")).thenReturn(secret);

        assertTrue(adapter.getSecret("empty").isEmpty());
        assertNull(cache.getIfPresent("empty"));
    }

    @Test
    void getSecret_devuelve_vacio_cuando_azure_responde_no_encontrado() {
        final HttpResponse response = responseWithStatus(404);
        when(secretClient.getSecret("ghost"))
                .thenThrow(new ResourceNotFoundException("not found", response));

        assertTrue(adapter.getSecret("ghost").isEmpty());
    }

    @Test
    void getSecret_traduce_error_ante_401_y_no_deja_valor_en_cache() {
        final HttpResponse response = responseWithStatus(401);
        when(secretClient.getSecret("jwt-key"))
                .thenThrow(new HttpResponseException("unauthorized", response));

        final SecretVaultException ex = assertThrows(SecretVaultException.class, () -> adapter.getSecret("jwt-key"));

        assertTrue(ex.getMessage().contains("jwt-key"));
        assertInstanceOf(HttpResponseException.class, ex.getCause());
        assertNull(cache.getIfPresent("jwt-key"));
    }

    @Test
    void getSecret_traduce_error_ante_403() {
        final HttpResponse response = responseWithStatus(403);
        when(secretClient.getSecret("jwt-key"))
                .thenThrow(new HttpResponseException("forbidden", response));

        final SecretVaultException ex = assertThrows(SecretVaultException.class, () -> adapter.getSecret("jwt-key"));

        assertInstanceOf(HttpResponseException.class, ex.getCause());
    }

    @Test
    void getSecret_traduce_error_http_distinto_de_autenticacion() {
        final HttpResponse response = responseWithStatus(500);
        when(secretClient.getSecret("jwt-key"))
                .thenThrow(new HttpResponseException("server error", response));

        final SecretVaultException ex = assertThrows(SecretVaultException.class, () -> adapter.getSecret("jwt-key"));

        assertTrue(ex.getMessage().contains("jwt-key"));
        assertInstanceOf(HttpResponseException.class, ex.getCause());
    }

    @Test
    void getSecret_traduce_error_http_sin_respuesta_asociada() {
        when(secretClient.getSecret("jwt-key"))
                .thenThrow(new HttpResponseException("sin respuesta", null));

        final SecretVaultException ex = assertThrows(SecretVaultException.class, () -> adapter.getSecret("jwt-key"));

        assertInstanceOf(HttpResponseException.class, ex.getCause());
    }

    @Test
    void getSecret_traduce_excepciones_inesperadas_del_proveedor() {
        final IllegalStateException failure = new IllegalStateException("boom");
        when(secretClient.getSecret("jwt-key")).thenThrow(failure);

        final SecretVaultException ex = assertThrows(SecretVaultException.class, () -> adapter.getSecret("jwt-key"));

        assertSame(failure, ex.getCause());
    }

    @Test
    void getRequiredSecret_devuelve_el_valor_cuando_existe() {
        final KeyVaultSecret secret = secretWithValue("abc");
        when(secretClient.getSecret("api-key")).thenReturn(secret);

        assertEquals("abc", adapter.getRequiredSecret("api-key"));
    }

    @Test
    void getRequiredSecret_lanza_not_found_cuando_no_existe() {
        when(secretClient.getSecret(anyString())).thenReturn(null);

        final SecretNotFoundException ex = assertThrows(SecretNotFoundException.class,
                () -> adapter.getRequiredSecret("api-key"));

        assertTrue(ex.getMessage().contains("api-key"));
    }

    @Test
    void invalidateSecret_ignora_nombres_nulos_o_en_blanco_y_mantiene_la_cache() {
        cache.put("api-key", "abc");

        adapter.invalidateSecret(null);
        adapter.invalidateSecret("  ");

        assertEquals("abc", cache.getIfPresent("api-key"));
    }

    @Test
    void invalidateSecret_elimina_solo_la_entrada_indicada() {
        cache.put("api-key", "abc");
        cache.put("other", "xyz");

        adapter.invalidateSecret(" api-key ");

        assertNull(cache.getIfPresent("api-key"));
        assertEquals("xyz", cache.getIfPresent("other"));
    }

    @Test
    void invalidateAll_vacia_la_cache_y_fuerza_nueva_consulta() {
        cache.put("api-key", "abc");
        cache.put("other", "xyz");
        final KeyVaultSecret secret = secretWithValue("nuevo");
        when(secretClient.getSecret("api-key")).thenReturn(secret);

        adapter.invalidateAll();

        assertNull(cache.getIfPresent("other"));
        assertEquals(Optional.of("nuevo"), adapter.getSecret("api-key"));
        verify(secretClient, never()).getSecret("other");
    }
}
