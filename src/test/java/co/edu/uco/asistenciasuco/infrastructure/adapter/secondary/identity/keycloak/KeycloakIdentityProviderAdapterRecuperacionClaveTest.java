package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.identity.keycloak;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU177: la contrasena que el usuario define desde el enlace de recuperacion queda definitiva (no temporal) y se
 * cierran las sesiones abiertas de la cuenta. Un fallo al cerrar sesiones no deshace el cambio.
 */
class KeycloakIdentityProviderAdapterRecuperacionClaveTest {

    private static final String REALM = "test-realm";
    private static final String EXTERNAL_ID = "kc-user-1";
    private static final String TOKEN_PATH = "/realms/" + REALM + "/protocol/openid-connect/token";
    private static final String USER_PATH = "/admin/realms/" + REALM + "/users/" + EXTERNAL_ID;
    private static final String RESET_PASSWORD_PATH = USER_PATH + "/reset-password";
    private static final String LOGOUT_PATH = USER_PATH + "/logout";
    private static final String CLAVE = "ClaveNueva-2026";

    private FakeKeycloakAdminServer server;
    private KeycloakIdentityProviderAdapter adapter;

    @BeforeEach
    void setUp() {
        server = new FakeKeycloakAdminServer();
        adapter = new KeycloakIdentityProviderAdapter(
                server.baseUrl(), REALM, "admin-client", "s3cr3t-value", "asistencias-api", "idUsuario");
        server.stubJson("POST", TOKEN_PATH, 200, "{\"access_token\":\"fake-admin-token\"}");
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void establecer_clave_la_deja_definitiva_y_cierra_las_sesiones() {
        server.stubNoContent("PUT", RESET_PASSWORD_PATH, 204);
        server.stubNoContent("POST", LOGOUT_PATH, 204);

        adapter.establecerClave(EXTERNAL_ID, CLAVE);

        final String cuerpo = server.lastRequestBody("PUT", RESET_PASSWORD_PATH).replace(" ", "");
        assertTrue(cuerpo.contains("\"type\":\"password\""), cuerpo);
        assertTrue(cuerpo.contains("\"temporary\":false"), cuerpo);
        assertTrue(server.wasCalled("POST", LOGOUT_PATH), "Debe cerrar las sesiones abiertas de la cuenta.");
    }

    @Test
    void un_fallo_al_cerrar_sesiones_no_deshace_el_cambio_de_clave() {
        server.stubNoContent("PUT", RESET_PASSWORD_PATH, 204);
        server.stubJson("POST", LOGOUT_PATH, 500, "{}");

        assertDoesNotThrow(() -> adapter.establecerClave(EXTERNAL_ID, CLAVE));
        assertTrue(server.wasCalled("PUT", RESET_PASSWORD_PATH));
    }

    @Test
    void si_keycloak_rechaza_la_clave_falla_de_forma_controlada_y_no_cierra_sesiones() {
        server.stubJson("PUT", RESET_PASSWORD_PATH, 400, "{\"error\":\"invalidPasswordMinLengthMessage\"}");

        assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.establecerClave(EXTERNAL_ID, CLAVE));
        assertFalse(server.wasCalled("POST", LOGOUT_PATH));
    }

    @Test
    void sin_cuenta_o_sin_clave_falla_sin_llamar_a_keycloak() {
        assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.establecerClave(" ", CLAVE));
        assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.establecerClave(EXTERNAL_ID, null));
        assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.establecerClave(EXTERNAL_ID, "  "));
        assertFalse(server.wasCalled("PUT", RESET_PASSWORD_PATH));
    }

    @Test
    void un_proveedor_que_no_implementa_la_operacion_falla_de_forma_controlada() {
        final IdentityProviderPort basico = org.mockito.Mockito.mock(
                IdentityProviderPort.class, org.mockito.Mockito.CALLS_REAL_METHODS);

        assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> basico.establecerClave(EXTERNAL_ID, CLAVE));
    }
}
