package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.azure;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort.ParameterCatalogException;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort.ParameterNotFoundException;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.core.http.HttpResponse;
import com.azure.data.appconfiguration.ConfigurationClient;
import com.azure.data.appconfiguration.models.ConfigurationSetting;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AzureAppConfigParameterCatalogAdapterTest {

    private ConfigurationClient configurationClient;
    private Cache<String, String> cache;
    private AzureAppConfigParameterCatalogAdapter adapter;

    @BeforeEach
    void setUp() {
        configurationClient = Mockito.mock(ConfigurationClient.class);
        cache = Caffeine.newBuilder().build();
        adapter = new AzureAppConfigParameterCatalogAdapter(configurationClient, cache);
    }

    private static ConfigurationSetting setting(final String key, final String value) {
        return new ConfigurationSetting().setKey(key).setValue(value);
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("not found", Mockito.mock(HttpResponse.class));
    }

    @Test
    void consulta_utiliza_cache_y_tras_invalidar_reconsulta_a_azure() {
        final AzureAppConfigParameterCatalogAdapter defaultCacheAdapter = new AzureAppConfigParameterCatalogAdapter(configurationClient);
        final ConfigurationSetting setting = setting("attendance:window", "15");
        when(configurationClient.getConfigurationSetting(eq("attendance:window"), isNull(String.class))).thenReturn(setting);

        final Optional<String> primero = defaultCacheAdapter.getParameter("attendance", "window");
        assertTrue(primero.isPresent());
        assertEquals("15", primero.get());
        verify(configurationClient, times(1)).getConfigurationSetting(eq("attendance:window"), isNull(String.class));

        final Optional<String> segundo = defaultCacheAdapter.getParameter("attendance", "window");
        assertTrue(segundo.isPresent());
        verify(configurationClient, times(1)).getConfigurationSetting(eq("attendance:window"), isNull(String.class));

        defaultCacheAdapter.invalidateParameter("attendance", "window");

        final Optional<String> tercero = defaultCacheAdapter.getParameter("attendance", "window");
        assertTrue(tercero.isPresent());
        verify(configurationClient, times(2)).getConfigurationSetting(eq("attendance:window"), isNull(String.class));
    }

    @Test
    void constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new AzureAppConfigParameterCatalogAdapter(null));
        assertThrows(NullPointerException.class, () -> new AzureAppConfigParameterCatalogAdapter(configurationClient, null));
    }

    // ------------------------------------------------------------------ getParameter

    @Test
    void getParameter_con_grupo_o_clave_nulos_o_en_blanco_devuelve_vacio_sin_consultar_azure() {
        assertTrue(adapter.getParameter(null, "k").isEmpty());
        assertTrue(adapter.getParameter("  ", "k").isEmpty());
        assertTrue(adapter.getParameter("g", null).isEmpty());
        assertTrue(adapter.getParameter("g", "").isEmpty());

        verifyNoInteractions(configurationClient);
    }

    @Test
    void getParameter_normaliza_grupo_y_clave_y_cachea_el_valor() {
        when(configurationClient.getConfigurationSetting(eq("g:k"), isNull(String.class))).thenReturn(setting("g:k", "valor"));

        assertEquals(Optional.of("valor"), adapter.getParameter(" g ", " k "));
        assertEquals("valor", cache.getIfPresent("g:k"));
    }

    @Test
    void getParameter_devuelve_vacio_cuando_azure_devuelve_setting_nulo() {
        when(configurationClient.getConfigurationSetting(eq("g:k"), isNull(String.class))).thenReturn(null);

        assertTrue(adapter.getParameter("g", "k").isEmpty());
    }

    @Test
    void getParameter_devuelve_vacio_cuando_el_setting_no_tiene_valor() {
        when(configurationClient.getConfigurationSetting(eq("g:k"), isNull(String.class))).thenReturn(setting("g:k", null));

        assertTrue(adapter.getParameter("g", "k").isEmpty());
        assertNull(cache.getIfPresent("g:k"));
    }

    @Test
    void getParameter_no_reintenta_cuando_no_existe_y_la_clave_alterna_es_identica() {
        when(configurationClient.getConfigurationSetting(eq("g:k"), isNull(String.class))).thenThrow(notFound());

        assertTrue(adapter.getParameter("g", "k").isEmpty());

        verify(configurationClient, times(1)).getConfigurationSetting(eq("g:k"), isNull(String.class));
    }

    @Test
    void getParameter_reintenta_con_clave_alterna_sin_normalizar_y_cachea_bajo_la_clave_normalizada() {
        when(configurationClient.getConfigurationSetting(eq("g:k"), isNull(String.class))).thenThrow(notFound());
        when(configurationClient.getConfigurationSetting(eq(" g : k "), isNull(String.class)))
                .thenReturn(setting(" g : k ", "alterno"));

        assertEquals(Optional.of("alterno"), adapter.getParameter(" g ", " k "));
        assertEquals("alterno", cache.getIfPresent("g:k"));
    }

    @Test
    void getParameter_devuelve_vacio_si_la_clave_alterna_tampoco_existe() {
        when(configurationClient.getConfigurationSetting(eq("g:k"), isNull(String.class))).thenThrow(notFound());
        when(configurationClient.getConfigurationSetting(eq(" g : k "), isNull(String.class))).thenThrow(notFound());

        assertTrue(adapter.getParameter(" g ", " k ").isEmpty());
    }

    @Test
    void getParameter_devuelve_vacio_si_la_clave_alterna_devuelve_setting_nulo_o_sin_valor() {
        when(configurationClient.getConfigurationSetting(eq("g:k"), isNull(String.class))).thenThrow(notFound());
        when(configurationClient.getConfigurationSetting(eq(" g : k "), isNull(String.class))).thenReturn(null);
        assertTrue(adapter.getParameter(" g ", " k ").isEmpty());

        when(configurationClient.getConfigurationSetting(eq(" g : k "), isNull(String.class)))
                .thenReturn(setting(" g : k ", null));
        assertTrue(adapter.getParameter(" g ", " k ").isEmpty());
        assertNull(cache.getIfPresent("g:k"));
    }

    @Test
    void getParameter_traduce_fallos_del_proveedor_a_parameter_catalog_exception() {
        final IllegalStateException failure = new IllegalStateException("azure caido");
        when(configurationClient.getConfigurationSetting(eq("g:k"), isNull(String.class))).thenThrow(failure);

        final ParameterCatalogException ex = assertThrows(ParameterCatalogException.class,
                () -> adapter.getParameter("g", "k"));

        assertTrue(ex.getMessage().contains("g:k"));
        assertSame(failure, ex.getCause());
    }

    // ------------------------------------------------------------------ getRequiredParameter

    @Test
    void getRequiredParameter_devuelve_el_valor_cuando_existe() {
        cache.put("g:k", "valor");

        assertEquals("valor", adapter.getRequiredParameter("g", "k"));
    }

    @Test
    void getRequiredParameter_lanza_not_found_cuando_no_existe() {
        when(configurationClient.getConfigurationSetting(eq("g:k"), isNull(String.class))).thenThrow(notFound());

        final ParameterNotFoundException ex = assertThrows(ParameterNotFoundException.class,
                () -> adapter.getRequiredParameter("g", "k"));

        assertTrue(ex.getMessage().contains("g:k"));
    }

    // ------------------------------------------------------------------ getParameterAs

    @Test
    void getParameterAs_convierte_a_string_sin_alterar_el_valor() {
        cache.put("g:k", " texto ");

        assertEquals(" texto ", adapter.getParameterAs("g", "k", String.class));
    }

    @Test
    void getParameterAs_convierte_a_enteros_de_wrapper_y_primitivo() {
        cache.put("g:k", " 15 ");

        assertEquals(15, adapter.getParameterAs("g", "k", Integer.class));
        assertEquals(15, adapter.getParameterAs("g", "k", int.class));
    }

    @Test
    void getParameterAs_convierte_a_long_de_wrapper_y_primitivo() {
        cache.put("g:k", "9000000000");

        assertEquals(9_000_000_000L, adapter.getParameterAs("g", "k", Long.class));
        assertEquals(9_000_000_000L, adapter.getParameterAs("g", "k", long.class));
    }

    @Test
    void getParameterAs_convierte_a_boolean_de_wrapper_y_primitivo() {
        cache.put("g:k", " true ");
        cache.put("g:f", "no-es-true");

        assertTrue(adapter.getParameterAs("g", "k", Boolean.class));
        assertTrue(adapter.getParameterAs("g", "k", boolean.class));
        assertFalse(adapter.getParameterAs("g", "f", Boolean.class));
    }

    @Test
    void getParameterAs_convierte_a_double_de_wrapper_y_primitivo() {
        cache.put("g:k", "2.5");

        assertEquals(2.5d, adapter.getParameterAs("g", "k", Double.class));
        assertEquals(2.5d, adapter.getParameterAs("g", "k", double.class));
    }

    @Test
    void getParameterAs_rechaza_tipos_destino_no_soportados() {
        cache.put("g:k", "1");

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> adapter.getParameterAs("g", "k", java.math.BigDecimal.class));

        assertTrue(ex.getMessage().contains("BigDecimal"));
    }

    @Test
    void getParameterAs_propaga_error_de_formato_numerico() {
        cache.put("g:k", "no-numerico");

        assertThrows(NumberFormatException.class, () -> adapter.getParameterAs("g", "k", Integer.class));
    }

    @Test
    void getParameterAs_lanza_not_found_cuando_el_parametro_no_existe() {
        when(configurationClient.getConfigurationSetting(eq("g:k"), isNull(String.class))).thenThrow(notFound());

        assertThrows(ParameterNotFoundException.class, () -> adapter.getParameterAs("g", "k", String.class));
    }

    // ------------------------------------------------------------------ invalidacion

    @Test
    void invalidateParameter_con_grupo_y_clave_elimina_la_entrada_normalizada() {
        cache.put("g:k", "valor");
        cache.put("g:otra", "otro");

        adapter.invalidateParameter(" g ", " k ");

        assertNull(cache.getIfPresent("g:k"));
        assertEquals("otro", cache.getIfPresent("g:otra"));
    }

    @Test
    void invalidateParameter_sin_grupo_invalida_la_clave_simple() {
        cache.put("clave-simple", "valor");

        adapter.invalidateParameter(null, " clave-simple ");
        assertNull(cache.getIfPresent("clave-simple"));

        cache.put("clave-simple", "valor");
        adapter.invalidateParameter("  ", "clave-simple");
        assertNull(cache.getIfPresent("clave-simple"));
    }

    @Test
    void invalidateParameter_sin_clave_valida_no_hace_nada() {
        cache.put("g:k", "valor");

        adapter.invalidateParameter("g", null);
        adapter.invalidateParameter("g", "  ");
        adapter.invalidateParameter(null, null);

        assertEquals("valor", cache.getIfPresent("g:k"));
    }

    @Test
    void invalidateAll_y_clearCache_vacian_la_cache_y_fuerzan_nueva_consulta() {
        cache.put("g:k", "viejo");
        when(configurationClient.getConfigurationSetting(eq("g:k"), isNull(String.class))).thenReturn(setting("g:k", "nuevo"));

        adapter.invalidateAll();
        assertEquals(Optional.of("nuevo"), adapter.getParameter("g", "k"));

        cache.put("g:k", "viejo");
        adapter.clearCache();
        assertNull(cache.getIfPresent("g:k"));
        verify(configurationClient, times(1)).getConfigurationSetting(eq("g:k"), isNull(String.class));
        verify(configurationClient, never()).getConfigurationSetting(eq("otra"), isNull(String.class));
    }
}
