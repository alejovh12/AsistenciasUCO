package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.identity.keycloak;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IDENT-002: una cuenta registrada por otra persona nace sin credencial; al activarla se consulta si ya tiene contrasena
 * y se establece una definitiva (no temporal) con la que define su titular.
 */
class KeycloakIdentityProviderAdapterActivacionCuentaTest {

    private static final String REALM = "test-realm";
    private static final String API_CLIENT_ID = "asistencias-api";
    private static final String USER_ID_ATTRIBUTE = "idUsuario";
    private static final UUID ID_USUARIO = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");
    private static final String EXTERNAL_ID = "kc-user-1";
    private static final String CLIENT_UUID = "kc-client-uuid-1";

    private static final String TOKEN_PATH = "/realms/" + REALM + "/protocol/openid-connect/token";
    private static final String USERS_PATH = "/admin/realms/" + REALM + "/users";
    private static final String USER_PATH = USERS_PATH + "/" + EXTERNAL_ID;
    private static final String SEARCH_PATH = USERS_PATH + "?q=" + USER_ID_ATTRIBUTE + "%3A" + ID_USUARIO;
    private static final String RESET_PASSWORD_PATH = USER_PATH + "/reset-password";
    private static final String CREDENTIALS_PATH = USER_PATH + "/credentials";
    private static final String CLIENTS_SEARCH_PATH = "/admin/realms/" + REALM + "/clients?clientId=" + API_CLIENT_ID;
    private static final String ROLE_PATH = "/admin/realms/" + REALM + "/clients/" + CLIENT_UUID + "/roles/DOCENTE";
    private static final String ROLE_MAPPINGS_PATH = USER_PATH + "/role-mappings/clients/" + CLIENT_UUID;

    private FakeKeycloakAdminServer server;
    private KeycloakIdentityProviderAdapter adapter;

    @BeforeEach
    void setUp() {
        server = new FakeKeycloakAdminServer();
        adapter = new KeycloakIdentityProviderAdapter(
                server.baseUrl(), REALM, "admin-client", "s3cr3t-value", API_CLIENT_ID, USER_ID_ATTRIBUTE);
        server.stubJson("POST", TOKEN_PATH, 200, "{\"access_token\":\"fake-admin-token\"}");
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void alta_sin_contrasena_crea_la_cuenta_deshabilitada_sin_llamar_a_reset_password() {
        server.stubJson("GET", SEARCH_PATH, 200, "[]");
        server.stubCreated("POST", USERS_PATH, server.baseUrl() + USERS_PATH + "/" + EXTERNAL_ID);
        server.stubNoContent("PUT", RESET_PASSWORD_PATH, 204);
        server.stubJson("GET", CLIENTS_SEARCH_PATH, 200, "[{\"id\":\"" + CLIENT_UUID + "\",\"clientId\":\"" + API_CLIENT_ID + "\"}]");
        server.stubJson("GET", ROLE_PATH, 200, "{\"id\":\"role-id-1\",\"name\":\"DOCENTE\",\"clientRole\":true}");
        server.stubJson("GET", ROLE_MAPPINGS_PATH, 200, "[]");
        server.stubNoContent("POST", ROLE_MAPPINGS_PATH, 204);

        final var cuenta = adapter.crearCuenta(new CrearCuentaIdentidadDTO("1017555444", ID_USUARIO, "jorge.henao@uco.edu.co",
                "Jorge", "Henao", null, InstitutionalRole.DOCENTE).conVerificacionCorreo());

        assertTrue(cuenta.newlyCreated());
        assertEquals(0, server.callCount("PUT", RESET_PASSWORD_PATH));
        final String cuerpo = server.lastRequestBody("POST", USERS_PATH).replace(" ", "");
        assertTrue(cuerpo.contains("\"enabled\":false"));
        assertFalse(cuerpo.contains("credentials"));
    }

    @Test
    void tiene_contrasena_si_alguna_credencial_es_de_tipo_password() {
        server.stubJson("GET", CREDENTIALS_PATH, 200, "[{\"id\":\"c1\",\"type\":\"otp\"},{\"id\":\"c2\",\"type\":\"password\"}]");

        assertTrue(adapter.tieneContrasena(EXTERNAL_ID));
    }

    @Test
    void sin_credenciales_o_solo_de_otro_tipo_no_tiene_contrasena() {
        server.stubJson("GET", CREDENTIALS_PATH, 200, "[]");
        assertFalse(adapter.tieneContrasena(EXTERNAL_ID));

        server.stubJson("GET", CREDENTIALS_PATH, 200, "[{\"id\":\"c1\",\"type\":\"otp\"}]");
        assertFalse(adapter.tieneContrasena(EXTERNAL_ID));
    }

    @Test
    void un_error_al_consultar_credenciales_es_fallo_del_proveedor() {
        server.stubJson("GET", CREDENTIALS_PATH, 500, "{}");

        assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.tieneContrasena(EXTERNAL_ID));
    }

    @Test
    void establecer_contrasena_la_envia_como_definitiva_no_temporal() {
        server.stubNoContent("PUT", RESET_PASSWORD_PATH, 204);

        adapter.establecerContrasena(EXTERNAL_ID, "MiClaveSegura2026!");

        final String cuerpo = server.lastRequestBody("PUT", RESET_PASSWORD_PATH).replace(" ", "");
        assertTrue(cuerpo.contains("\"type\":\"password\""));
        assertTrue(cuerpo.contains("\"temporary\":false"));
        assertTrue(cuerpo.contains("MiClaveSegura2026!"));
    }

    @Test
    void establecer_contrasena_vacia_o_con_error_del_proveedor_falla() {
        assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.establecerContrasena(EXTERNAL_ID, " "));
        assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.establecerContrasena(EXTERNAL_ID, null));
        assertEquals(0, server.callCount("PUT", RESET_PASSWORD_PATH));

        server.stubNoContent("PUT", RESET_PASSWORD_PATH, 400);
        assertThrows(IdentityProviderPort.IdentityProviderException.class,
                () -> adapter.establecerContrasena(EXTERNAL_ID, "MiClaveSegura2026!"));
    }
}
