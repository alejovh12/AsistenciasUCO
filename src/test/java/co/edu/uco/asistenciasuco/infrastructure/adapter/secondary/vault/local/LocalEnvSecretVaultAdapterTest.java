package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.vault.local;

import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort.SecretNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalEnvSecretVaultAdapterTest {

    private static final String PROPERTY_NAME = "uco.test.local-vault.secret";
    private static final String MISSING_SECRET = "uco-secret-que-no-existe";

    @AfterEach
    void clearSystemProperty() {
        System.clearProperty(PROPERTY_NAME);
    }

    @Test
    void getSecret_con_nombre_nulo_o_en_blanco_devuelve_vacio() {
        final LocalEnvSecretVaultAdapter adapter = new LocalEnvSecretVaultAdapter();

        assertTrue(adapter.getSecret(null).isEmpty());
        assertTrue(adapter.getSecret("").isEmpty());
        assertTrue(adapter.getSecret("   ").isEmpty());
    }

    @Test
    void constructor_con_overrides_nulos_equivale_a_sin_overrides() {
        final LocalEnvSecretVaultAdapter adapter = new LocalEnvSecretVaultAdapter(null);

        assertTrue(adapter.getSecret(MISSING_SECRET).isEmpty());
    }

    @Test
    void override_local_tiene_prioridad_sobre_propiedad_del_sistema() {
        System.setProperty(PROPERTY_NAME, "desde-propiedad");
        final LocalEnvSecretVaultAdapter adapter = new LocalEnvSecretVaultAdapter(Map.of(PROPERTY_NAME, "desde-override"));

        assertEquals(Optional.of("desde-override"), adapter.getSecret(PROPERTY_NAME));
    }

    @Test
    void override_con_valor_nulo_se_interpreta_como_vacio_sin_consultar_otras_fuentes() {
        System.setProperty(PROPERTY_NAME, "desde-propiedad");
        final Map<String, String> overrides = new HashMap<>();
        overrides.put(PROPERTY_NAME, null);
        final LocalEnvSecretVaultAdapter adapter = new LocalEnvSecretVaultAdapter(overrides);

        assertTrue(adapter.getSecret(PROPERTY_NAME).isEmpty());
    }

    @Test
    void propiedad_del_sistema_se_usa_cuando_no_hay_override() {
        System.setProperty(PROPERTY_NAME, "desde-propiedad");
        final LocalEnvSecretVaultAdapter adapter = new LocalEnvSecretVaultAdapter();

        assertEquals(Optional.of("desde-propiedad"), adapter.getSecret(PROPERTY_NAME));
    }

    @Test
    void propiedad_del_sistema_en_blanco_se_ignora() {
        System.setProperty(PROPERTY_NAME, "   ");
        final LocalEnvSecretVaultAdapter adapter = new LocalEnvSecretVaultAdapter();

        assertTrue(adapter.getSecret(PROPERTY_NAME).isEmpty());
    }

    @Test
    void variable_de_entorno_se_resuelve_normalizando_el_nombre_a_mayusculas() {
        // PATH existe en cualquier entorno de ejecucion; el nombre en minuscula comprueba la normalizacion.
        final LocalEnvSecretVaultAdapter adapter = new LocalEnvSecretVaultAdapter();

        final Optional<String> value = adapter.getSecret("path");

        assertTrue(value.isPresent());
        assertEquals(System.getenv("PATH"), value.get());
    }

    @Test
    void secreto_inexistente_en_todas_las_fuentes_devuelve_vacio() {
        final LocalEnvSecretVaultAdapter adapter = new LocalEnvSecretVaultAdapter();

        assertTrue(adapter.getSecret(MISSING_SECRET).isEmpty());
    }

    @Test
    void getRequiredSecret_devuelve_el_valor_cuando_existe() {
        final LocalEnvSecretVaultAdapter adapter = new LocalEnvSecretVaultAdapter(Map.of("db-password", "s3cr3t"));

        assertEquals("s3cr3t", adapter.getRequiredSecret("db-password"));
    }

    @Test
    void getRequiredSecret_lanza_not_found_cuando_no_existe() {
        final LocalEnvSecretVaultAdapter adapter = new LocalEnvSecretVaultAdapter();

        final SecretNotFoundException ex = assertThrows(SecretNotFoundException.class,
                () -> adapter.getRequiredSecret(MISSING_SECRET));

        assertTrue(ex.getMessage().contains(MISSING_SECRET));
    }
}
