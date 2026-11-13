package co.edu.uco.asistenciasuco.infrastructure.config.adapters.vault.runtime;

import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SEC-02 / SEC-12 (VAL-oleada3-baul-secretos): carga de secretos de ejecucion desde el baul con un puerto falso.
 * Nunca usa Azure real; los valores son ficticios de prueba.
 */
class VaultRuntimeSecretsEnvironmentPostProcessorTest {

    private static final String ENDPOINT = "https://baul-de-prueba.invalid/";
    private static final Map<String, String> CATALOGO = new LinkedHashMap<>();

    static {
        CATALOGO.put("spring.datasource.password", "asistencias-db-password");
        CATALOGO.put("app.providers.minio.secret-key", "asistencias-minio-secret-key");
        CATALOGO.put("app.security.azure-events.webhook-token", "asistencias-eventgrid-webhook-token");
    }

    private final List<String> endpointsSolicitados = new ArrayList<>();

    private VaultRuntimeSecretsEnvironmentPostProcessor procesador(final SecretVaultPort baul) {
        final Function<String, SecretVaultPort> fabrica = endpoint -> {
            endpointsSolicitados.add(endpoint);
            return baul;
        };
        return new VaultRuntimeSecretsEnvironmentPostProcessor(new DeferredLogs(), fabrica, CATALOGO);
    }

    private static MockEnvironment ambienteAzure() {
        final MockEnvironment environment = new MockEnvironment();
        environment.setProperty("app.adapters.vault.provider", "azure_keyvault");
        environment.setProperty("app.providers.azure-keyvault.endpoint", ENDPOINT);
        return environment;
    }

    @Test
    void con_proveedor_azure_los_secretos_del_baul_tienen_prioridad_y_los_ausentes_caen_a_la_variable_de_entorno() {
        final MockEnvironment environment = ambienteAzure();
        environment.setProperty("spring.datasource.password", "valor-de-variable-de-entorno");
        environment.setProperty("app.providers.minio.secret-key", "minio-desde-variable");

        procesador(new BaulEnMemoria(Map.of(
                "asistencias-db-password", "valor-del-baul",
                "asistencias-eventgrid-webhook-token", "token-del-baul"
        ))).postProcessEnvironment(environment, null);

        assertEquals(List.of(ENDPOINT), endpointsSolicitados);
        assertEquals(VaultSecretPropertySource.NOMBRE, environment.getPropertySources().iterator().next().getName());
        assertEquals("valor-del-baul", environment.getProperty("spring.datasource.password"));
        assertEquals("token-del-baul", environment.getProperty("app.security.azure-events.webhook-token"));
        assertEquals("minio-desde-variable", environment.getProperty("app.providers.minio.secret-key"),
                "un secreto ausente en el baul conserva el valor de la variable de entorno");
    }

    @Test
    void proveedor_local_env_no_consulta_el_baul_ni_agrega_fuentes() {
        final MockEnvironment environment = new MockEnvironment();
        environment.setProperty("app.adapters.vault.provider", "local_env");
        final int fuentesAntes = environment.getPropertySources().size();

        procesador(new BaulEnMemoria(Map.of())).postProcessEnvironment(environment, null);

        assertTrue(endpointsSolicitados.isEmpty());
        assertEquals(fuentesAntes, environment.getPropertySources().size());
    }

    @Test
    void sin_selector_de_proveedor_no_hace_nada() {
        final MockEnvironment environment = new MockEnvironment();

        procesador(new BaulEnMemoria(Map.of())).postProcessEnvironment(environment, null);

        assertTrue(endpointsSolicitados.isEmpty());
        assertNull(environment.getPropertySources().get(VaultSecretPropertySource.NOMBRE));
    }

    @Test
    void la_bandera_desactiva_solo_la_carga_de_secretos() {
        final MockEnvironment environment = ambienteAzure();
        environment.setProperty("app.secrets.runtime-from-vault.enabled", "false");

        procesador(new BaulEnMemoria(Map.of("asistencias-db-password", "x"))).postProcessEnvironment(environment, null);

        assertTrue(endpointsSolicitados.isEmpty());
        assertNull(environment.getPropertySources().get(VaultSecretPropertySource.NOMBRE));
    }

    @Test
    void sec12_proveedor_azure_sin_endpoint_falla_al_arrancar_con_mensaje_claro() {
        final MockEnvironment environment = new MockEnvironment();
        environment.setProperty("app.adapters.vault.provider", "azure_keyvault");
        environment.setProperty("app.providers.azure-keyvault.endpoint", "  ");

        final IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> procesador(new BaulEnMemoria(Map.of())).postProcessEnvironment(environment, null));

        assertTrue(error.getMessage().contains("AZURE_KEYVAULT_ENDPOINT"));
        assertTrue(error.getMessage().contains("local_env"));
        assertTrue(endpointsSolicitados.isEmpty());
    }

    @Test
    void un_fallo_tecnico_del_baul_detiene_el_arranque_sin_exponer_valores() {
        final MockEnvironment environment = ambienteAzure();
        final SecretVaultPort baulCaido = new BaulEnMemoria(Map.of()) {
            @Override
            public Optional<String> getSecret(final String secretName) {
                throw new SecretVaultException("sin red");
            }
        };

        final IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> procesador(baulCaido).postProcessEnvironment(environment, null));

        assertTrue(error.getMessage().contains("asistencias-db-password"));
        assertTrue(error.getMessage().contains("spring.datasource.password"));
        assertNotNull(error.getCause());
    }

    @Test
    void un_secreto_en_blanco_se_trata_como_ausente() {
        final VaultSecretPropertySource fuente = VaultSecretPropertySource.cargar(
                new BaulEnMemoria(Map.of("asistencias-db-password", "   ")), CATALOGO);

        assertEquals(0, fuente.getPropertyNames().length);
    }

    @Test
    void corre_despues_de_cargar_application_yml() {
        assertTrue(procesador(new BaulEnMemoria(Map.of())).getOrder() > ConfigDataEnvironmentPostProcessor.ORDER);
    }

    @Test
    void el_constructor_de_spring_usa_el_catalogo_completo_y_no_abre_conexiones() {
        final MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addLast(new MapPropertySource("vacio", new HashMap<>()));

        new VaultRuntimeSecretsEnvironmentPostProcessor(new DeferredLogs()).postProcessEnvironment(environment, null);

        assertNull(environment.getPropertySources().get(VaultSecretPropertySource.NOMBRE));
    }

    @Test
    void esta_registrado_en_spring_factories() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("META-INF/spring.factories")) {
            assertNotNull(in, "Falta META-INF/spring.factories");
            final String contenido = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(contenido.contains("org.springframework.boot.EnvironmentPostProcessor"));
            assertTrue(contenido.contains(VaultRuntimeSecretsEnvironmentPostProcessor.class.getName()));
        }
    }

    @Test
    void la_fuente_no_incluye_propiedades_fuera_del_catalogo() {
        final VaultSecretPropertySource fuente = VaultSecretPropertySource.cargar(
                new BaulEnMemoria(Map.of("asistencias-db-password", "v", "otro-secreto", "w")), CATALOGO);

        assertEquals(1, fuente.getPropertyNames().length);
        assertFalse(fuente.containsProperty("otro-secreto"));
    }

    /** Puerto falso: secretos en memoria, sin SDK ni red. */
    private static class BaulEnMemoria implements SecretVaultPort {

        private final Map<String, String> secretos;

        BaulEnMemoria(final Map<String, String> secretos) {
            this.secretos = secretos;
        }

        @Override
        public Optional<String> getSecret(final String secretName) {
            return Optional.ofNullable(secretos.get(secretName));
        }

        @Override
        public String getRequiredSecret(final String secretName) {
            return getSecret(secretName).orElseThrow(() -> new SecretNotFoundException(secretName));
        }
    }
}
