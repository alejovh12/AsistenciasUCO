package co.edu.uco.asistenciasuco.infrastructure.config.adapters.vault.runtime;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SEC-02: el catalogo cubre los secretos de ejecucion pedidos, usa nombres validos de Key Vault y coincide con el
 * inventario documentado.
 */
class RuntimeSecretCatalogTest {

    private static final Pattern NOMBRE_KEY_VAULT = Pattern.compile("^[0-9a-zA-Z-]{1,127}$");
    private static final Path INVENTARIO = Path.of("docs", "security", "SECRETS_INVENTORY.md");

    @Test
    void cubre_bd_keycloak_pdp_minio_smtp_y_webhook() {
        final List<String> requeridas = List.of(
                "spring.datasource.password",
                "app.providers.keycloak-identity.admin-client-secret",
                "app.providers.security-baseline-identity.admin-client-secret",
                "app.providers.security-baseline-pdp-admin.client-secret",
                "app.providers.security-baseline-pep.application-credential",
                "app.providers.minio.access-key",
                "app.providers.minio.secret-key",
                "app.providers.smtp-notification.password",
                "app.security.azure-events.webhook-token",
                "app.providers.catalogo-parametros.secreto");

        assertTrue(RuntimeSecretCatalog.PROPIEDAD_A_SECRETO.keySet().containsAll(requeridas));
    }

    @Test
    void los_nombres_son_validos_en_key_vault_y_unicos() {
        RuntimeSecretCatalog.PROPIEDAD_A_SECRETO.values()
                .forEach(nombre -> assertTrue(NOMBRE_KEY_VAULT.matcher(nombre).matches(), nombre));
        assertEquals(RuntimeSecretCatalog.PROPIEDAD_A_SECRETO.size(),
                new HashSet<>(RuntimeSecretCatalog.PROPIEDAD_A_SECRETO.values()).size());
    }

    @Test
    void el_catalogo_es_inmutable() {
        assertThrows(UnsupportedOperationException.class,
                () -> RuntimeSecretCatalog.PROPIEDAD_A_SECRETO.put("x", "y"));
    }

    @Test
    void el_inventario_documentado_lista_cada_propiedad_y_secreto() throws IOException {
        final String inventario = Files.readString(INVENTARIO, StandardCharsets.UTF_8);
        RuntimeSecretCatalog.PROPIEDAD_A_SECRETO.forEach((propiedad, secreto) -> {
            assertTrue(inventario.contains("`" + propiedad + "`"), "Falta en el inventario: " + propiedad);
            assertTrue(inventario.contains("`" + secreto + "`"), "Falta en el inventario: " + secreto);
        });
    }
}
