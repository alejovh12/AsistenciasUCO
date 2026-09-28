package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.azure;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort.MessageCatalogException;
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

class AzureAppConfigMessageCatalogAdapterTest {

    private static final String USER_KEY = "messages:user:VAL-001";
    private static final String TECHNICAL_KEY = "messages:technical:VAL-001";

    private ConfigurationClient configurationClient;
    private Cache<String, String> userCache;
    private Cache<String, String> technicalCache;
    private AzureAppConfigMessageCatalogAdapter adapter;

    @BeforeEach
    void setUp() {
        configurationClient = Mockito.mock(ConfigurationClient.class);
        userCache = Caffeine.newBuilder().build();
        technicalCache = Caffeine.newBuilder().build();
        adapter = new AzureAppConfigMessageCatalogAdapter(configurationClient, userCache, technicalCache);
    }

    private static ConfigurationSetting setting(final String key, final String value) {
        return new ConfigurationSetting().setKey(key).setValue(value);
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("not found", Mockito.mock(HttpResponse.class));
    }

    @Test
    void consulta_utiliza_cache_y_tras_invalidar_reconsulta_a_azure() {
        final AzureAppConfigMessageCatalogAdapter defaultCacheAdapter = new AzureAppConfigMessageCatalogAdapter(configurationClient);
        final ConfigurationSetting setting = setting(USER_KEY, "El campo {0} es obligatorio");
        when(configurationClient.getConfigurationSetting(eq(USER_KEY), eq("es"))).thenReturn(setting);

        final Optional<String> primero = defaultCacheAdapter.findUserMessage("VAL-001");
        assertTrue(primero.isPresent());
        assertEquals("El campo {0} es obligatorio", primero.get());
        verify(configurationClient, times(1)).getConfigurationSetting(eq(USER_KEY), eq("es"));

        final Optional<String> segundo = defaultCacheAdapter.findUserMessage("VAL-001");
        assertTrue(segundo.isPresent());
        verify(configurationClient, times(1)).getConfigurationSetting(eq(USER_KEY), eq("es"));

        defaultCacheAdapter.invalidateMessage("VAL-001");

        final Optional<String> tercero = defaultCacheAdapter.findUserMessage("VAL-001");
        assertTrue(tercero.isPresent());
        verify(configurationClient, times(2)).getConfigurationSetting(eq(USER_KEY), eq("es"));
    }

    @Test
    void constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new AzureAppConfigMessageCatalogAdapter(null));
        assertThrows(NullPointerException.class,
                () -> new AzureAppConfigMessageCatalogAdapter(configurationClient, null, technicalCache));
        assertThrows(NullPointerException.class,
                () -> new AzureAppConfigMessageCatalogAdapter(configurationClient, userCache, null));
    }

    // ------------------------------------------------------------------ findUserMessage

    @Test
    void findUserMessage_con_codigo_nulo_o_en_blanco_devuelve_vacio_sin_consultar_azure() {
        assertTrue(adapter.findUserMessage(null).isEmpty());
        assertTrue(adapter.findUserMessage("").isEmpty());
        assertTrue(adapter.findUserMessage("   ").isEmpty());

        verifyNoInteractions(configurationClient);
    }

    @Test
    void findUserMessage_normaliza_el_codigo_y_cachea_el_valor() {
        when(configurationClient.getConfigurationSetting(USER_KEY, "es")).thenReturn(setting(USER_KEY, "Obligatorio"));

        assertEquals(Optional.of("Obligatorio"), adapter.findUserMessage("  VAL-001  "));
        assertEquals("Obligatorio", userCache.getIfPresent("VAL-001"));
    }

    @Test
    void findUserMessage_usa_fallback_sin_label_cuando_no_existe_con_label_es() {
        when(configurationClient.getConfigurationSetting(USER_KEY, "es")).thenThrow(notFound());
        when(configurationClient.getConfigurationSetting(eq(USER_KEY), isNull(String.class)))
                .thenReturn(setting(USER_KEY, "Sin label"));

        assertEquals(Optional.of("Sin label"), adapter.findUserMessage("VAL-001"));
        assertEquals("Sin label", userCache.getIfPresent("VAL-001"));
    }

    @Test
    void findUserMessage_devuelve_vacio_cuando_no_existe_ni_con_label_ni_sin_label() {
        when(configurationClient.getConfigurationSetting(USER_KEY, "es")).thenThrow(notFound());
        when(configurationClient.getConfigurationSetting(eq(USER_KEY), isNull(String.class))).thenThrow(notFound());

        assertTrue(adapter.findUserMessage("VAL-001").isEmpty());
        assertNull(userCache.getIfPresent("VAL-001"));
    }

    @Test
    void findUserMessage_devuelve_vacio_cuando_azure_devuelve_setting_nulo() {
        when(configurationClient.getConfigurationSetting(USER_KEY, "es")).thenReturn(null);

        assertTrue(adapter.findUserMessage("VAL-001").isEmpty());
    }

    @Test
    void findUserMessage_devuelve_vacio_cuando_el_setting_no_tiene_valor() {
        when(configurationClient.getConfigurationSetting(USER_KEY, "es")).thenReturn(setting(USER_KEY, null));

        assertTrue(adapter.findUserMessage("VAL-001").isEmpty());
        assertNull(userCache.getIfPresent("VAL-001"));
    }

    @Test
    void findUserMessage_traduce_fallos_del_proveedor_a_message_catalog_exception() {
        final IllegalStateException failure = new IllegalStateException("azure caido");
        when(configurationClient.getConfigurationSetting(USER_KEY, "es")).thenThrow(failure);

        final MessageCatalogException ex = assertThrows(MessageCatalogException.class,
                () -> adapter.findUserMessage("VAL-001"));

        assertTrue(ex.getMessage().contains("VAL-001"));
        assertSame(failure, ex.getCause());
    }

    @Test
    void findUserMessage_traduce_fallos_del_fallback_sin_label() {
        final IllegalStateException failure = new IllegalStateException("azure caido");
        when(configurationClient.getConfigurationSetting(USER_KEY, "es")).thenThrow(notFound());
        when(configurationClient.getConfigurationSetting(eq(USER_KEY), isNull(String.class))).thenThrow(failure);

        final MessageCatalogException ex = assertThrows(MessageCatalogException.class,
                () -> adapter.findUserMessage("VAL-001"));

        assertSame(failure, ex.getCause());
    }

    // ------------------------------------------------------------------ findTechnicalMessage

    @Test
    void findTechnicalMessage_con_codigo_nulo_o_en_blanco_devuelve_vacio_sin_consultar_azure() {
        assertTrue(adapter.findTechnicalMessage(null).isEmpty());
        assertTrue(adapter.findTechnicalMessage(" ").isEmpty());

        verifyNoInteractions(configurationClient);
    }

    @Test
    void findTechnicalMessage_consulta_azure_una_vez_y_luego_usa_cache() {
        when(configurationClient.getConfigurationSetting(eq(TECHNICAL_KEY), isNull(String.class)))
                .thenReturn(setting(TECHNICAL_KEY, "detalle tecnico"));

        assertEquals(Optional.of("detalle tecnico"), adapter.findTechnicalMessage(" VAL-001 "));
        assertEquals(Optional.of("detalle tecnico"), adapter.findTechnicalMessage("VAL-001"));

        verify(configurationClient, times(1)).getConfigurationSetting(eq(TECHNICAL_KEY), isNull(String.class));
    }

    @Test
    void findTechnicalMessage_devuelve_vacio_cuando_azure_devuelve_setting_nulo_o_sin_valor() {
        when(configurationClient.getConfigurationSetting(eq(TECHNICAL_KEY), isNull(String.class))).thenReturn(null);
        assertTrue(adapter.findTechnicalMessage("VAL-001").isEmpty());

        when(configurationClient.getConfigurationSetting(eq(TECHNICAL_KEY), isNull(String.class)))
                .thenReturn(setting(TECHNICAL_KEY, null));
        assertTrue(adapter.findTechnicalMessage("VAL-001").isEmpty());
        assertNull(technicalCache.getIfPresent("VAL-001"));
    }

    @Test
    void findTechnicalMessage_devuelve_vacio_cuando_no_existe_en_azure() {
        when(configurationClient.getConfigurationSetting(eq(TECHNICAL_KEY), isNull(String.class))).thenThrow(notFound());

        assertTrue(adapter.findTechnicalMessage("VAL-001").isEmpty());
    }

    @Test
    void findTechnicalMessage_traduce_fallos_del_proveedor() {
        final IllegalStateException failure = new IllegalStateException("azure caido");
        when(configurationClient.getConfigurationSetting(eq(TECHNICAL_KEY), isNull(String.class))).thenThrow(failure);

        final MessageCatalogException ex = assertThrows(MessageCatalogException.class,
                () -> adapter.findTechnicalMessage("VAL-001"));

        assertTrue(ex.getMessage().contains("VAL-001"));
        assertSame(failure, ex.getCause());
    }

    // ------------------------------------------------------------------ getUserMessage / getTechnicalMessage

    @Test
    void getUserMessage_formatea_la_plantilla_encontrada_con_argumentos_indexados() {
        userCache.put("VAL-001", "El campo {0} debe tener {1} caracteres");

        assertEquals("El campo nombre debe tener 5 caracteres", adapter.getUserMessage("VAL-001", "nombre", 5));
    }

    @Test
    void getUserMessage_formatea_plantillas_con_marcadores_vacios_y_argumentos_nulos() {
        userCache.put("VAL-001", "Campo {} invalido: {}");

        assertEquals("Campo nombre invalido: null", adapter.getUserMessage("VAL-001", "nombre", null));
    }

    @Test
    void getUserMessage_sin_argumentos_devuelve_la_plantilla_sin_formatear() {
        userCache.put("VAL-001", "Valor {0} pendiente");

        assertEquals("Valor {0} pendiente", adapter.getUserMessage("VAL-001"));
    }

    @Test
    void getUserMessage_con_argumentos_nulos_devuelve_la_plantilla() {
        userCache.put("VAL-001", "Valor {0} pendiente");

        assertEquals("Valor {0} pendiente", adapter.getUserMessage("VAL-001", (Object[]) null));
    }

    @Test
    void getUserMessage_devuelve_el_codigo_cuando_el_mensaje_no_existe() {
        when(configurationClient.getConfigurationSetting(USER_KEY, "es")).thenThrow(notFound());
        when(configurationClient.getConfigurationSetting(eq(USER_KEY), isNull(String.class))).thenThrow(notFound());

        assertEquals("VAL-001", adapter.getUserMessage("VAL-001", "x"));
    }

    @Test
    void getUserMessage_devuelve_la_plantilla_original_cuando_el_patron_es_invalido() {
        userCache.put("VAL-001", "Patron roto {0");

        assertEquals("Patron roto {0", adapter.getUserMessage("VAL-001", "x"));
    }

    @Test
    void getUserMessage_con_plantilla_en_blanco_no_intenta_formatear() {
        userCache.put("VAL-001", "   ");

        assertEquals("   ", adapter.getUserMessage("VAL-001", "x"));
    }

    @Test
    void getTechnicalMessage_formatea_el_mensaje_tecnico() {
        technicalCache.put("VAL-001", "Fallo en {0}");

        assertEquals("Fallo en modulo", adapter.getTechnicalMessage("VAL-001", "modulo"));
    }

    @Test
    void getTechnicalMessage_devuelve_el_codigo_cuando_no_existe_mensaje() {
        when(configurationClient.getConfigurationSetting(eq(TECHNICAL_KEY), isNull(String.class))).thenThrow(notFound());

        assertEquals("VAL-001", adapter.getTechnicalMessage("VAL-001", "modulo"));
    }

    // ------------------------------------------------------------------ invalidacion

    @Test
    void invalidateMessage_elimina_la_entrada_de_ambas_caches() {
        userCache.put("VAL-001", "usuario");
        technicalCache.put("VAL-001", "tecnico");
        userCache.put("VAL-002", "otro");

        adapter.invalidateMessage(" VAL-001 ");

        assertNull(userCache.getIfPresent("VAL-001"));
        assertNull(technicalCache.getIfPresent("VAL-001"));
        assertEquals("otro", userCache.getIfPresent("VAL-002"));
    }

    @Test
    void invalidateMessage_ignora_codigos_nulos_o_en_blanco() {
        userCache.put("VAL-001", "usuario");

        adapter.invalidateMessage(null);
        adapter.invalidateMessage("  ");

        assertEquals("usuario", userCache.getIfPresent("VAL-001"));
    }

    @Test
    void invalidateAll_y_clearCache_vacian_ambas_caches() {
        userCache.put("VAL-001", "usuario");
        technicalCache.put("VAL-001", "tecnico");

        adapter.invalidateAll();

        assertNull(userCache.getIfPresent("VAL-001"));
        assertNull(technicalCache.getIfPresent("VAL-001"));

        userCache.put("VAL-002", "usuario");
        technicalCache.put("VAL-002", "tecnico");

        adapter.clearCache();

        assertNull(userCache.getIfPresent("VAL-002"));
        assertNull(technicalCache.getIfPresent("VAL-002"));
        verify(configurationClient, never()).getConfigurationSetting(Mockito.anyString(), Mockito.anyString());
    }
}
