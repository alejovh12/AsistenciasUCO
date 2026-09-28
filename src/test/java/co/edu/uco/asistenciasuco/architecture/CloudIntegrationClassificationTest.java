package co.edu.uco.asistenciasuco.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TD-053: una prueba que lee recursos Azure reales (secretos/parámetros desde un contexto que
 * cablea los adapters cloud) es Cloud Integration y no puede llevar un nombre que Surefire
 * ejecute en {@code mvn verify}; debe terminar en {@code CloudIntegrationIT}.
 */
class CloudIntegrationClassificationTest {

    private static final Path TEST_SOURCES = Path.of("src", "test", "java");
    private static final String SELF = "CloudIntegrationClassificationTest.java";

    @Test
    void pruebas_que_leen_azure_real_solo_existen_como_CloudIntegrationIT() throws IOException {
        final List<String> offenders;
        try (Stream<Path> files = Files.walk(TEST_SOURCES)) {
            offenders = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals(SELF))
                    .filter(path -> !path.getFileName().toString().endsWith("CloudIntegrationIT.java"))
                    .filter(path -> readsRealAzure(read(path)))
                    .map(path -> path.getFileName().toString())
                    .toList();
        }

        assertTrue(offenders.isEmpty(), "Cloud Integration debe llamarse *CloudIntegrationIT: " + offenders);
    }

    @Test
    void el_guard_detecta_la_IT_actual_si_se_renombrara_a_Test() {
        final String currentIt = read(TEST_SOURCES.resolve(Path.of("co", "edu", "uco", "asistenciasuco",
                "infrastructure", "adapter", "secondary", "azure", "AzureCloudIntegrationIT.java")));

        assertTrue(readsRealAzure(currentIt), "El contenido actual de la IT Azure debe clasificarse como Cloud Integration");
    }

    @Test
    void el_guard_no_marca_como_cloud_un_test_de_wiring_sin_endpoints_reales() {
        final String wiringOnly = read(TEST_SOURCES.resolve(Path.of("co", "edu", "uco", "asistenciasuco",
                "infrastructure", "config", "adapters", "vault", "VaultAdapterConfigurationTest.java")));

        assertFalse(readsRealAzure(wiringOnly), "VaultAdapterConfigurationTest solo valida wiring y fail-fast");
    }

    private static boolean readsRealAzure(final String source) {
        final boolean wiredThroughContext = source.contains("AzureKeyVaultAdapterConfiguration")
                && source.contains("ApplicationContextRunner")
                && source.contains(".getSecret(");
        final boolean operationalEndpoint = source.contains("AZURE_KEYVAULT_ENDPOINT")
                || source.contains("AZURE_APPCONFIG_ENDPOINT")
                || source.contains(".vault.azure.net");
        return wiredThroughContext && operationalEndpoint;
    }

    private static String read(final Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (final IOException exception) {
            throw new IllegalStateException("No se pudo leer " + path.getFileName(), exception);
        }
    }
}
