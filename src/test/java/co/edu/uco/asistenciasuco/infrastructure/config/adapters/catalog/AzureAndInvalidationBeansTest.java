package co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog;

import co.edu.uco.asistenciasuco.application.features.admin.procesareventoazure.primaryports.ProcesarEventoAzureInputPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.CatalogInvalidationPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.azure.AzureAppConfigParameterCatalogAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.composite.CompositeCatalogInvalidationAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.vault.azure.AzureKeyVaultAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.azure.AzureAppConfigAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.invalidation.CatalogInvalidationAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.vault.azure.AzureKeyVaultAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.AzureAppConfigProviderProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.AzureKeyVaultProviderProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

/**
 * Composition Root de los proveedores Azure y del wiring de invalidacion: cada bean construye el
 * adaptador correcto sin conectarse a Azure (los builders del SDK no hacen red al construirse).
 */
class AzureAndInvalidationBeansTest {

    @Test
    void azure_app_config_configuration_construye_el_catalogo_de_parametros_azure() {
        final ParameterCatalogPort port = new AzureAppConfigAdapterConfiguration()
                .parameterCatalogPort(new AzureAppConfigProviderProperties("https://uco-test.azconfig.io"));

        assertInstanceOf(AzureAppConfigParameterCatalogAdapter.class, port);
    }

    @Test
    void azure_key_vault_configuration_construye_el_vault_azure() {
        final SecretVaultPort port = new AzureKeyVaultAdapterConfiguration()
                .secretVaultPort(new AzureKeyVaultProviderProperties("https://uco-test.vault.azure.net"));

        assertInstanceOf(AzureKeyVaultAdapter.class, port);
    }

    @Test
    void invalidation_configuration_compone_el_adaptador_de_invalidacion_y_el_caso_de_uso_de_eventos() {
        final CatalogInvalidationAdapterConfiguration configuration = new CatalogInvalidationAdapterConfiguration();

        final CatalogInvalidationPort invalidationPort = configuration.catalogInvalidationPort(
                mock(ParameterCatalogPort.class), mock(MessageCatalogPort.class), mock(SecretVaultPort.class));
        final ProcesarEventoAzureInputPort inputPort = configuration.procesarEventoAzureInputPort(
                invalidationPort, mock(RealtimePublisherPort.class));

        assertInstanceOf(CompositeCatalogInvalidationAdapter.class, invalidationPort);
        assertInstanceOf(ProcesarEventoAzureInputPort.class, inputPort);
    }
}
