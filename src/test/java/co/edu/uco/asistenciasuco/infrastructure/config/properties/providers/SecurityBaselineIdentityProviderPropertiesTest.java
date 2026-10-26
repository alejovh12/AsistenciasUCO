package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityBaselineIdentityProviderPropertiesTest {

    private static SecurityBaselineIdentityProviderProperties create(final String url, final String realm, final String secret) {
        return new SecurityBaselineIdentityProviderProperties(
                url, realm, "asistencias-provisioner", secret, "asistencias-api", "idUsuario");
    }

    @Test
    void acepta_el_realm_central_y_no_expone_el_secreto() {
        final var properties = create("http://localhost:19090", "security-baseline", "test-only-secret");

        assertFalse(properties.toString().contains("test-only-secret"));
    }

    @Test
    void rechaza_el_keycloak_legacy_por_puerto_o_realm() {
        assertThrows(IllegalStateException.class, () -> create("http://localhost:8081", "security-baseline", "s"));
        assertThrows(IllegalStateException.class, () -> create("http://localhost:19090", "asistencias-uco", "s"));
    }

    @Test
    void exige_secreto_administrativo_sin_valor_por_defecto() {
        assertThrows(IllegalStateException.class, () -> create("http://localhost:19090", "security-baseline", ""));
        assertThrows(IllegalStateException.class, () -> create("http://localhost:19090", "security-baseline", null));
    }
}
