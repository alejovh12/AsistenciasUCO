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

class AzureAppConfigParameterCatalogAdapterTest {

    private ConfigurationClient configurationClient;
    private AzureAppConfigParameterCatalogAdapter adapter;

    @BeforeEach
    void setUp() {
        configurationClient = Mockito.mock(ConfigurationClient.class);
        adapter = new AzureAppConfigParameterCatalogAdapter(configurationClient);
    }

    @Test
    void consulta_utiliza_cache_y_tras_invalidar_reconsulta_a_azure() {
        final ConfigurationSetting setting = new ConfigurationSetting().setKey("asistencias:max_inasistencias").setValue("20");
        when(configurationClient.getConfigurationSetting(eq("asistencias:max_inasistencias"), any()))
                .thenReturn(setting);

        final Optional<String> primero = adapter.getParameter("asistencias", "max_inasistencias");
        assertTrue(primero.isPresent());
        assertEquals("20", primero.get());
        verify(configurationClient, times(1)).getConfigurationSetting(eq("asistencias:max_inasistencias"), any());

        final Optional<String> segundo = adapter.getParameter("asistencias", "max_inasistencias");
        assertTrue(segundo.isPresent());
        assertEquals("20", segundo.get());
        verify(configurationClient, times(1)).getConfigurationSetting(eq("asistencias:max_inasistencias"), any());

        adapter.invalidateParameter("asistencias", "max_inasistencias");

        final Optional<String> tercero = adapter.getParameter("asistencias", "max_inasistencias");
        assertTrue(tercero.isPresent());
        assertEquals("20", tercero.get());
        verify(configurationClient, times(2)).getConfigurationSetting(eq("asistencias:max_inasistencias"), any());
    }
}