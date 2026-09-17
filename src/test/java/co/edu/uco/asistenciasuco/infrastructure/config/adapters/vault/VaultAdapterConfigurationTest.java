package co.edu.uco.asistenciasuco.infrastructure.config.adapters.vault;

import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.vault.local.LocalEnvSecretVaultAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.vault.azure.AzureKeyVaultAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.vault.local.LocalEnvVaultAdapterConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaultAdapterConfigurationTest {

    private final ApplicationContextRunner localContextRunner = new ApplicationContextRunner()
            .withUserConfiguration(LocalEnvVaultAdapterConfiguration.class)
            .withPropertyValues(
                    "app.adapters.vault.provider=local_env"
            );

    private final ApplicationContextRunner missingConfigRunner = new ApplicationContextRunner()
            .withUserConfiguration(AzureKeyVaultAdapterConfiguration.class)
            .withPropertyValues(
                    "app.adapters.vault.provider=azure_keyvault",
                    "app.providers.azure-keyvault.endpoint="
            );

    @Test
    void activa_local_env_vault_adapter_cuando_provider_es_local_env() {
        localContextRunner.run(context -> {
            assertEquals(1, context.getBeansOfType(SecretVaultPort.class).size());
            assertInstanceOf(LocalEnvSecretVaultAdapter.class, context.getBean(SecretVaultPort.class));

            final SecretVaultPort port = context.getBean(SecretVaultPort.class);
            assertTrue(port.getSecret("invalido").isEmpty());
        });
    }

    @Test
    void falla_fail_fast_si_endpoint_azure_keyvault_esta_vacio() {
        missingConfigRunner.run(context -> {
            assertTrue(context.getStartupFailure() != null);
        });
    }
}
