package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.azure;

import com.azure.data.appconfiguration.ConfigurationClient;
import com.azure.data.appconfiguration.models.ConfigurationSetting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AzureAppConfigMessageCatalogAdapterTest {

    private ConfigurationClient configurationClient;
    private AzureAppConfigMessageCatalogAdapter adapter;

    @BeforeEach
    void setUp() {
        configurationClient = Mockito.mock(ConfigurationClient.class);
        adapter = new AzureAppConfigMessageCatalogAdapter(configurationClient);
    }

    @Test
    void consulta_utiliza_cache_y_tras_invalidar_reconsulta_a_azure() {
        final ConfigurationSetting setting = new ConfigurationSetting()
                .setKey("messages:user:VAL-001")
                .setValue("El campo {0} es obligatorio");

        when(configurationClient.getConfigurationSetting(eq("messages:user:VAL-001"), eq("es")))
                .thenReturn(setting);

        final Optional<String> primero = adapter.findUserMessage("VAL-001");
        assertTrue(primero.isPresent());
        assertEquals("El campo {0} es obligatorio", primero.get());
        verify(configurationClient, times(1)).getConfigurationSetting(eq("messages:user:VAL-001"), eq("es"));

        final Optional<String> segundo = adapter.findUserMessage("VAL-001");
        assertTrue(segundo.isPresent());
        verify(configurationClient, times(1)).getConfigurationSetting(eq("messages:user:VAL-001"), eq("es"));

        adapter.invalidateMessage("VAL-001");

        final Optional<String> tercero = adapter.findUserMessage("VAL-001");
        assertTrue(tercero.isPresent());
        verify(configurationClient, times(2)).getConfigurationSetting(eq("messages:user:VAL-001"), eq("es"));
    }
}