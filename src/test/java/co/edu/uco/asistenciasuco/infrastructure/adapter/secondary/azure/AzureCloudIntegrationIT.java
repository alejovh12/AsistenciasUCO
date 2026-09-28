package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.azure;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.azure.AzureAppConfigAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.azure.AzureAppConfigMessageCatalogAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.vault.azure.AzureKeyVaultAdapterConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Cloud Integration (Azure real, SOLO LECTURA). No forma parte de {@code mvn verify}: se ejecuta
 * únicamente con {@code .\mvnw.cmd -Pazure-integration verify} en un ambiente autorizado
 * ({@code DefaultAzureCredential}, p. ej. {@code az login}) y con
 * {@code AZURE_KEYVAULT_ENDPOINT} / {@code AZURE_APPCONFIG_ENDPOINT} definidos.
 *
 * <p>Si falta el ambiente la prueba FALLA de forma explícita; nunca se salta ni pasa en falso.
 * Lee mediante los Ports neutrales; no crea, rota ni borra recursos y no imprime valores.
 * No certifica Event Grid real ni invalidación end-to-end (MV-003 sigue pendiente).</p>
 */
class AzureCloudIntegrationIT {

    private static final String KEYVAULT_ENDPOINT_VARIABLE = "AZURE_KEYVAULT_ENDPOINT";
    private static final String APPCONFIG_ENDPOINT_VARIABLE = "AZURE_APPCONFIG_ENDPOINT";

    private static String requiredEndpoint(final String name) {
        final String value = System.getProperty(name, System.getenv(name));
        if (value == null || value.isBlank()) {
            return fail("Ambiente Azure no disponible: defina " + name + " (endpoint del recurso, no un secreto).");
        }
        return value.trim();
    }

    @Test
    @DisplayName("Debe leer secreto de Key Vault, parámetro de App Configuration y mensaje con label es desde Azure")
    void debe_consumir_recursos_reales_de_azure_mediante_puertos_secundarios() {
        final ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(
                        AzureKeyVaultAdapterConfiguration.class,
                        AzureAppConfigAdapterConfiguration.class,
                        AzureAppConfigMessageCatalogAdapterConfiguration.class
                )
                .withPropertyValues(
                        "app.adapters.vault.provider=azure_keyvault",
                        "app.providers.azure-keyvault.endpoint=" + requiredEndpoint(KEYVAULT_ENDPOINT_VARIABLE),
                        "app.adapters.parameter-catalog.provider=azure_appconfig",
                        "app.adapters.message-catalog.provider=azure",
                        "app.providers.azure-appconfig.endpoint=" + requiredEndpoint(APPCONFIG_ENDPOINT_VARIABLE)
                );

        runner.run(context -> {
            assertNull(context.getStartupFailure(), "El contexto Azure debe iniciar (identidad/endpoint válidos).");

            // 1. SecretVaultPort <- Azure Key Vault (solo existencia / no vacío; el valor no se imprime)
            final SecretVaultPort vaultPort = context.getBean(SecretVaultPort.class);
            assertNotNull(vaultPort, "SecretVaultPort debe estar registrado");
            final Optional<String> dbPassword = vaultPort.getSecret("database-password");
            assertTrue(dbPassword.isPresent(), "El secreto database-password debe existir en Key Vault");
            assertFalse(dbPassword.get().isBlank(), "El secreto database-password no debe estar vacío");

            // 2. ParameterCatalogPort <- Azure App Configuration
            final ParameterCatalogPort paramPort = context.getBean(ParameterCatalogPort.class);
            assertNotNull(paramPort, "ParameterCatalogPort debe estar registrado");
            final Optional<String> tolerancia = paramPort.getParameter("asistencias:asistencia", "tolerancia-minutos");
            assertTrue(tolerancia.isPresent(), "El parámetro de tolerancia debe existir en App Configuration");
            final Integer toleranciaMinutos = paramPort.getParameterAs("asistencias:asistencia", "tolerancia-minutos", Integer.class);
            assertNotNull(toleranciaMinutos);

            // 3. MessageCatalogPort <- Azure App Configuration
            final MessageCatalogPort messagePort = context.getBean(MessageCatalogPort.class);
            assertNotNull(messagePort, "MessageCatalogPort debe estar registrado");
            final String userMsg = messagePort.getUserMessage("CORR_001");
            assertNotNull(userMsg);
            assertTrue(userMsg.contains("identificador de correlación") || userMsg.contains("correlacion"),
                    "El mensaje de usuario obtenido debe contener el texto esperado cargado en Azure");
        });
    }
}
