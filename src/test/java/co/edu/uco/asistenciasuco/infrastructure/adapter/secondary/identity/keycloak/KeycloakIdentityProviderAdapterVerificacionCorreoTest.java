package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.identity.keycloak;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IDENT-001 BE-I01..BE-I03: el estudiante registrado por el docente se crea deshabilitado y con el correo sin verificar;
 * al verificar el correo se habilita y se marca verificado. Sin el indicador, el payload es identico al vigente.
 */
class KeycloakIdentityProviderAdapterVerificacionCorreoTest {

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
    private static final String CLIENTS_SEARCH_PATH = "/admin/realms/" + REALM + "/clients?clientId=" + API_CLIENT_ID;
    private static final String ROLE_PATH = "/admin/realms/" + REALM + "/clients/" + CLIENT_UUID + "/roles/ESTUDIANTE";
    private static final String ROLE_MAPPINGS_PATH = USER_PATH + "/role-mappings/clients/" + CLIENT_UUID;

    private FakeKeycloakAdminServer server;
    private KeycloakIdentityProviderAdapter adapter;

    @BeforeEach
    void setUp() {
        server = new FakeKeycloakAdminServer();
        adapter = new KeycloakIdentityProviderAdapter(
                server.baseUrl(), REALM, "admin-client", "s3cr3t-value", API_CLIENT_ID, USER_ID_ATTRIBUTE);
        server.stubJson("POST", TOKEN_PATH, 200, "{\"access_token\":\"fake-admin-token\"}");
        server.stubJson("GET", SEARCH_PATH, 200, "[]");
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    private void stubCreacionExitosa() {
        server.stubCreated("POST", USERS_PATH, server.baseUrl() + USERS_PATH + "/" + EXTERNAL_ID);
        server.stubNoContent("PUT", RESET_PASSWORD_PATH, 204);
        server.stubJson("GET", CLIENTS_SEARCH_PATH, 200, "[{\"id\":\"" + CLIENT_UUID + "\",\"clientId\":\"" + API_CLIENT_ID + "\"}]");
        server.stubJson("GET", ROLE_PATH, 200, "{\"id\":\"role-id-1\",\"name\":\"ESTUDIANTE\",\"clientRole\":true}");
        server.stubJson("GET", ROLE_MAPPINGS_PATH, 200, "[]");
        server.stubNoContent("POST", ROLE_MAPPINGS_PATH, 204);
    }

    private static CrearCuentaIdentidadDTO dto(final boolean requiereVerificacion) {
        return new CrearCuentaIdentidadDTO("123456789", ID_USUARIO, "ana.perez@uco.edu.co", "Ana", "Perez",
                "ClaveInicial123!", InstitutionalRole.ESTUDIANTE, requiereVerificacion);
    }

    private String cuerpoCreacion() {
        return server.lastRequestBody("POST", USERS_PATH).replace(" ", "");
    }

    @Test
    void BE_I01_con_verificacion_pendiente_la_cuenta_se_crea_deshabilitada_y_con_correo_sin_verificar() {
        stubCreacionExitosa();

        adapter.crearCuenta(dto(true));

        final String cuerpo = cuerpoCreacion();
        assertTrue(cuerpo.contains("\"enabled\":false"), cuerpo);
        assertTrue(cuerpo.contains("\"emailVerified\":false"), cuerpo);
        assertTrue(server.wasCalled("PUT", RESET_PASSWORD_PATH), "La contrasena se sigue estableciendo como hoy.");
    }

    @Test
    void BE_I01_sin_verificacion_pendiente_el_payload_es_el_vigente() {
        stubCreacionExitosa();

        adapter.crearCuenta(dto(false));

        final String cuerpo = cuerpoCreacion();
        assertTrue(cuerpo.contains("\"enabled\":true"), cuerpo);
        assertTrue(cuerpo.contains("\"emailVerified\":true"), cuerpo);
    }

    @Test
    void BE_I01_el_constructor_de_siete_campos_equivale_a_sin_verificacion_pendiente() {
        stubCreacionExitosa();

        adapter.crearCuenta(new CrearCuentaIdentidadDTO("123456789", ID_USUARIO, "ana.perez@uco.edu.co", "Ana", "Perez",
                "ClaveInicial123!", InstitutionalRole.ESTUDIANTE));

        final String cuerpo = cuerpoCreacion();
        assertTrue(cuerpo.contains("\"enabled\":true"), cuerpo);
        assertTrue(cuerpo.contains("\"emailVerified\":true"), cuerpo);
    }

    @Test
    void BE_I02_una_cuenta_existente_no_se_modifica_aunque_se_pida_verificacion() {
        server.stubJson("GET", SEARCH_PATH, 200,
                "[{\"id\":\"" + EXTERNAL_ID + "\",\"username\":\"123456789\",\"email\":\"ana.perez@uco.edu.co\","
                        + "\"attributes\":{\"" + USER_ID_ATTRIBUTE + "\":[\"" + ID_USUARIO + "\"]}}]");
        server.stubJson("GET", CLIENTS_SEARCH_PATH, 200, "[{\"id\":\"" + CLIENT_UUID + "\",\"clientId\":\"" + API_CLIENT_ID + "\"}]");
        server.stubJson("GET", ROLE_PATH, 200, "{\"id\":\"role-id-1\",\"name\":\"ESTUDIANTE\",\"clientRole\":true}");
        server.stubJson("GET", ROLE_MAPPINGS_PATH, 200, "[]");
        server.stubNoContent("POST", ROLE_MAPPINGS_PATH, 204);

        adapter.crearCuenta(dto(true));

        assertFalse(server.wasCalled("POST", USERS_PATH));
        assertFalse(server.wasCalled("PUT", USER_PATH));
        assertFalse(server.wasCalled("PUT", RESET_PASSWORD_PATH));
    }

    @Test
    void BE_I03_confirmar_correo_habilita_la_cuenta_y_marca_el_correo_verificado() {
        server.stubNoContent("PUT", USER_PATH, 204);

        adapter.confirmarCorreoCuenta(EXTERNAL_ID);

        final String cuerpo = server.lastRequestBody("PUT", USER_PATH).replace(" ", "");
        assertTrue(cuerpo.contains("\"enabled\":true"), cuerpo);
        assertTrue(cuerpo.contains("\"emailVerified\":true"), cuerpo);
    }

    @Test
    void BE_I03_confirmar_correo_es_idempotente() {
        server.stubNoContent("PUT", USER_PATH, 204);

        assertDoesNotThrow(() -> {
            adapter.confirmarCorreoCuenta(EXTERNAL_ID);
            adapter.confirmarCorreoCuenta(EXTERNAL_ID);
        });
    }

    @Test
    void BE_I03_confirmar_correo_de_cuenta_inexistente_falla() {
        server.stubNoContent("PUT", USER_PATH, 404);

        assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.confirmarCorreoCuenta(EXTERNAL_ID));
    }

    @Test
    void BE_I03_confirmar_correo_con_id_externo_invalido_no_llama_a_keycloak() {
        assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.confirmarCorreoCuenta(" "));
        assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.confirmarCorreoCuenta("a/b"));
        assertFalse(server.wasCalled("POST", TOKEN_PATH));
    }

    @Test
    void BE_I03_el_puerto_por_defecto_no_soporta_confirmar_correo() {
        final IdentityProviderPort sinSoporte = new IdentityProviderPort() {
            @Override
            public co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO crearCuenta(
                    final CrearCuentaIdentidadDTO dto) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void eliminarCuenta(final String idExterno) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void asignarRol(final String idExterno, final InstitutionalRole rol) {
                throw new UnsupportedOperationException();
            }
        };

        assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> sinSoporte.confirmarCorreoCuenta(EXTERNAL_ID));
    }
}
