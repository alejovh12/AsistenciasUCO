package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.identity;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba {@link KeycloakIdentityProviderAdapter} de punta a punta contra un
 * {@link FakeKeycloakAdminServer} (HTTP real, sin dependencias nuevas) — el adapter construye
 * su propio {@code HttpClient} internamente, así que no hay forma de mockearlo sin un servidor
 * real. No se conecta a ningún Keycloak real: solo a {@code 127.0.0.1} en un puerto efímero.
 */
class KeycloakIdentityProviderAdapterTest {

    private static final String REALM = "test-realm";
    private static final String ADMIN_CLIENT_ID = "admin-client";
    private static final String ADMIN_CLIENT_SECRET = "s3cr3t-value";
    private static final String API_CLIENT_ID = "asistencias-api";
    private static final String USER_ID_ATTRIBUTE = "idUsuario";
    private static final UUID ID_USUARIO = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");
    private static final String USERNAME = "jperez";
    private static final String EMAIL = "jperez@uco.edu.co";
    private static final String EXTERNAL_ID = "kc-user-1";
    private static final String CLIENT_UUID = "kc-client-uuid-1";
    private static final InstitutionalRole ROLE = InstitutionalRole.DOCENTE;
    private static final String ROLE_NAME = ROLE.name();

    private static final String USERS_PATH = "/admin/realms/" + REALM + "/users";
    private static final String TOKEN_PATH = "/realms/" + REALM + "/protocol/openid-connect/token";
    private static final String SEARCH_PATH = USERS_PATH + "?q=" + USER_ID_ATTRIBUTE + "%3A" + ID_USUARIO;
    private static final String RESET_PASSWORD_PATH = USERS_PATH + "/" + EXTERNAL_ID + "/reset-password";
    private static final String CLIENTS_SEARCH_PATH = "/admin/realms/" + REALM + "/clients?clientId=" + API_CLIENT_ID;
    private static final String ROLE_PATH = "/admin/realms/" + REALM + "/clients/" + CLIENT_UUID + "/roles/" + ROLE_NAME;
    private static final String ROLE_MAPPINGS_PATH =
            USERS_PATH + "/" + EXTERNAL_ID + "/role-mappings/clients/" + CLIENT_UUID;
    private static final String ASSIGN_ROLE_PATH = ROLE_MAPPINGS_PATH;
    private static final String DELETE_PATH = USERS_PATH + "/" + EXTERNAL_ID;

    private FakeKeycloakAdminServer server;
    private KeycloakIdentityProviderAdapter adapter;

    @BeforeEach
    void setUp() {
        server = new FakeKeycloakAdminServer();
        adapter = new KeycloakIdentityProviderAdapter(
                server.baseUrl(), REALM, ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET, API_CLIENT_ID, USER_ID_ATTRIBUTE
        );
        server.stubJson("POST", TOKEN_PATH, 200, "{\"access_token\":\"fake-admin-token\"}");
        server.stubJson("GET", SEARCH_PATH, 200, "[]");
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    private static CrearCuentaIdentidadDTO nuevoUsuarioDto() {
        return new CrearCuentaIdentidadDTO(USERNAME, ID_USUARIO, EMAIL, "Juan", "Perez", "ClaveInicial123!", ROLE);
    }

    private void stubCreacionExitosaCompleta() {
        server.stubCreated("POST", USERS_PATH, server.baseUrl() + USERS_PATH + "/" + EXTERNAL_ID);
        server.stubNoContent("PUT", RESET_PASSWORD_PATH, 204);
        server.stubJson("GET", CLIENTS_SEARCH_PATH, 200,
                "[{\"id\":\"" + CLIENT_UUID + "\",\"clientId\":\"" + API_CLIENT_ID + "\"}]");
        server.stubJson("GET", ROLE_PATH, 200,
                "{\"id\":\"role-1\",\"name\":\"" + ROLE_NAME + "\",\"clientRole\":true,\"containerId\":\"" + CLIENT_UUID + "\"}");
        server.stubJson("GET", ROLE_MAPPINGS_PATH, 200, "[]");
        server.stubNoContent("POST", ASSIGN_ROLE_PATH, 204);
    }

    private void stubCreacionHastaPassword() {
        server.stubCreated("POST", USERS_PATH, server.baseUrl() + USERS_PATH + "/" + EXTERNAL_ID);
        server.stubNoContent("PUT", RESET_PASSWORD_PATH, 204);
    }

    private void stubConflicto409ConExistente(final String username, final String email, final String idUsuarioValue) {
        server.stubNoContent("POST", USERS_PATH, 409);
        server.stubFirstJson("GET", SEARCH_PATH, 200, "[]");
        server.stubJson("GET", SEARCH_PATH, 200,
                "[{\"id\":\"" + EXTERNAL_ID + "\",\"username\":\"" + username + "\",\"email\":\"" + email
                        + "\",\"attributes\":{\"" + USER_ID_ATTRIBUTE + "\":[\"" + idUsuarioValue + "\"]}}]");
    }

    private void stubRoleNoAsignado() {
        server.stubJson("GET", CLIENTS_SEARCH_PATH, 200,
                "[{\"id\":\"" + CLIENT_UUID + "\",\"clientId\":\"" + API_CLIENT_ID + "\"}]");
        server.stubJson("GET", ROLE_PATH, 200,
                "{\"id\":\"role-1\",\"name\":\"" + ROLE_NAME + "\",\"clientRole\":true,\"containerId\":\"" + CLIENT_UUID + "\"}");
        server.stubJson("GET", ROLE_MAPPINGS_PATH, 200, "[]");
        server.stubNoContent("POST", ASSIGN_ROLE_PATH, 204);
    }

    private void stubRoleYaAsignado() {
        server.stubJson("GET", CLIENTS_SEARCH_PATH, 200,
                "[{\"id\":\"" + CLIENT_UUID + "\",\"clientId\":\"" + API_CLIENT_ID + "\"}]");
        server.stubJson("GET", ROLE_PATH, 200,
                "{\"id\":\"role-1\",\"name\":\"" + ROLE_NAME + "\",\"clientRole\":true,\"containerId\":\"" + CLIENT_UUID + "\"}");
        server.stubJson("GET", ROLE_MAPPINGS_PATH, 200,
                "[{\"id\":\"role-1\",\"name\":\"" + ROLE_NAME + "\",\"clientRole\":true,\"containerId\":\"" + CLIENT_UUID + "\"}]");
    }

    private String existingUserJson(final String username, final String email) {
        return "[{\"id\":\"" + EXTERNAL_ID + "\",\"username\":\"" + username
                + "\",\"email\":\"" + email + "\",\"attributes\":{\""
                + USER_ID_ATTRIBUTE + "\":[\"" + ID_USUARIO + "\"]}}]";
    }

    @Nested
    class InstitutionalIdentityLookupTests {

        @Test
        void cuenta_existente_por_idUsuario_se_reutiliza_sin_post_ni_password_y_asegura_role() {
            server.stubJson("GET", SEARCH_PATH, 200, existingUserJson(USERNAME, EMAIL));
            stubRoleNoAsignado();

            final CuentaIdentidadDTO resultado = adapter.crearCuenta(nuevoUsuarioDto());

            assertFalse(resultado.newlyCreated());
            assertEquals(EXTERNAL_ID, resultado.idExterno());
            assertEquals(1, server.callCount("GET", SEARCH_PATH));
            assertEquals(0, server.callCount("POST", USERS_PATH));
            assertEquals(0, server.callCount("PUT", RESET_PASSWORD_PATH));
            assertEquals(1, server.callCount("POST", ASSIGN_ROLE_PATH));
        }

        @Test
        void cero_identidades_por_idUsuario_crea_cuenta_con_password_y_role() {
            stubCreacionExitosaCompleta();

            final CuentaIdentidadDTO resultado = adapter.crearCuenta(nuevoUsuarioDto());

            assertTrue(resultado.newlyCreated());
            assertEquals(1, server.callCount("GET", SEARCH_PATH));
            assertEquals(1, server.callCount("POST", USERS_PATH));
            assertEquals(1, server.callCount("PUT", RESET_PASSWORD_PATH));
            assertEquals(1, server.callCount("POST", ASSIGN_ROLE_PATH));
        }

        @Test
        void busqueda_usa_userIdAttribute_configurable() {
            final String attribute = "institutional-id";
            adapter = new KeycloakIdentityProviderAdapter(
                    server.baseUrl(), REALM, ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET, API_CLIENT_ID, attribute
            );
            final String configuredPath = USERS_PATH + "?q=" + attribute + "%3A" + ID_USUARIO;
            server.stubJson("GET", configuredPath, 200, "[]");
            stubCreacionExitosaCompleta();

            adapter.crearCuenta(nuevoUsuarioDto());

            assertEquals(1, server.callCount("GET", configuredPath));
            assertEquals(0, server.callCount("GET", SEARCH_PATH));
            assertTrue(server.lastRequestBody("POST", USERS_PATH).contains("\"" + attribute + "\""));
        }

        @Test
        void dos_identidades_por_idUsuario_fallan_sin_post() {
            server.stubJson("GET", SEARCH_PATH, 200,
                    "[{\"id\":\"a\"},{\"id\":\"b\"}]");

            assertThrows(IdentityProviderPort.IdentityProviderException.class,
                    () -> adapter.crearCuenta(nuevoUsuarioDto()));

            assertEquals(0, server.callCount("POST", USERS_PATH));
        }

        @Test
        void drift_username_falla_sin_crear_ni_borrar() {
            server.stubJson("GET", SEARCH_PATH, 200, existingUserJson("username-anterior", EMAIL));

            assertThrows(IdentityProviderPort.IdentityProviderException.class,
                    () -> adapter.crearCuenta(nuevoUsuarioDto()));

            assertEquals(0, server.callCount("POST", USERS_PATH));
            assertEquals(0, server.callCount("DELETE", DELETE_PATH));
            assertEquals(0, server.callCount("PUT", RESET_PASSWORD_PATH));
        }

        @Test
        void drift_correo_falla_sin_crear_ni_borrar() {
            server.stubJson("GET", SEARCH_PATH, 200, existingUserJson(USERNAME, "correo-anterior@uco.edu.co"));

            assertThrows(IdentityProviderPort.IdentityProviderException.class,
                    () -> adapter.crearCuenta(nuevoUsuarioDto()));

            assertEquals(0, server.callCount("POST", USERS_PATH));
            assertEquals(0, server.callCount("DELETE", DELETE_PATH));
            assertEquals(0, server.callCount("PUT", RESET_PASSWORD_PATH));
        }

        @Test
        void race_409_resuelve_idUsuario_y_no_resetea_password() {
            server.stubFirstJson("GET", SEARCH_PATH, 200, "[]");
            server.stubJson("GET", SEARCH_PATH, 200, existingUserJson(USERNAME, EMAIL));
            server.stubNoContent("POST", USERS_PATH, 409);
            stubRoleNoAsignado();

            final CuentaIdentidadDTO resultado = adapter.crearCuenta(nuevoUsuarioDto());

            assertFalse(resultado.newlyCreated());
            assertEquals(2, server.callCount("GET", SEARCH_PATH));
            assertEquals(1, server.callCount("POST", USERS_PATH));
            assertEquals(0, server.callCount("PUT", RESET_PASSWORD_PATH));
            assertEquals(1, server.callCount("POST", ASSIGN_ROLE_PATH));
        }

        @Test
        void race_409_sin_vinculo_institucional_unico_falla() {
            server.stubNoContent("POST", USERS_PATH, 409);

            assertThrows(IdentityProviderPort.IdentityProviderException.class,
                    () -> adapter.crearCuenta(nuevoUsuarioDto()));

            assertEquals(2, server.callCount("GET", SEARCH_PATH));
            assertEquals(0, server.callCount("DELETE", DELETE_PATH));
        }
    }

    // -------------------------------------------------------------------------
    // Bloque 35: admin token
    // -------------------------------------------------------------------------

    @Nested
    class AdminTokenTests {

        @Test
        void client_credentials_correcto_envia_credenciales_esperadas_y_nunca_password_grant() {
            stubCreacionExitosaCompleta();

            adapter.crearCuenta(nuevoUsuarioDto());

            final String tokenRequestBody = server.lastRequestBody("POST", TOKEN_PATH);
            assertNotNull(tokenRequestBody);
            assertTrue(tokenRequestBody.contains("grant_type=client_credentials"));
            assertTrue(tokenRequestBody.contains("client_id=" + ADMIN_CLIENT_ID));
            assertTrue(tokenRequestBody.contains("client_secret=" + ADMIN_CLIENT_SECRET));
            assertFalse(tokenRequestBody.contains("grant_type=password"));
        }

        @Test
        void token_endpoint_no_2xx_lanza_excepcion_sin_exponer_secreto() {
            server.stub("POST", TOKEN_PATH, 401, "{\"error\":\"unauthorized_client\"}",
                    Map.of("Content-Type", "application/json"));

            final IdentityProviderPort.IdentityProviderException exception = assertThrows(
                    IdentityProviderPort.IdentityProviderException.class,
                    () -> adapter.crearCuenta(nuevoUsuarioDto())
            );

            assertFalse(exception.getMessage().contains(ADMIN_CLIENT_SECRET));
        }

        @Test
        void access_token_ausente_lanza_excepcion() {
            server.stubJson("POST", TOKEN_PATH, 200, "{}");

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));
        }

        @Test
        void access_token_blank_lanza_excepcion() {
            server.stubJson("POST", TOKEN_PATH, 200, "{\"access_token\":\"   \"}");

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));
        }

        @Test
        void respuesta_json_invalida_lanza_excepcion() {
            server.stub("POST", TOKEN_PATH, 200, "{esto-no-es-json", Map.of("Content-Type", "application/json"));

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));
        }
    }

    // -------------------------------------------------------------------------
    // Bloque 36: creación de usuario nuevo
    // -------------------------------------------------------------------------

    @Nested
    class CreationTests {

        @Test
        void usuario_nuevo_201_retorna_newlyCreated_true_con_external_id_desde_location() {
            stubCreacionExitosaCompleta();

            final CuentaIdentidadDTO resultado = adapter.crearCuenta(nuevoUsuarioDto());

            assertTrue(resultado.newlyCreated());
            assertEquals(EXTERNAL_ID, resultado.idExterno());
        }

        @Test
        void idUsuario_attribute_se_envia_con_nombre_configurado() {
            stubCreacionExitosaCompleta();

            adapter.crearCuenta(nuevoUsuarioDto());

            final String body = server.lastRequestBody("POST", USERS_PATH);
            assertNotNull(body);
            assertTrue(body.contains("\"" + USER_ID_ATTRIBUTE + "\""));
            assertTrue(body.contains(ID_USUARIO.toString()));
        }

        @Test
        void externalId_nunca_se_asume_igual_a_idUsuario() {
            stubCreacionExitosaCompleta();

            final CuentaIdentidadDTO resultado = adapter.crearCuenta(nuevoUsuarioDto());

            assertFalse(resultado.idExterno().equals(ID_USUARIO.toString()));
        }

        @Test
        void ausencia_de_location_header_en_201_recupera_identidad_exacta_como_nueva() {
            server.stub("POST", USERS_PATH, 201, "", Map.of());
            server.stubFirstJson("GET", SEARCH_PATH, 200, "[]");
            server.stubJson("GET", SEARCH_PATH, 200,
                    "[{\"id\":\"" + EXTERNAL_ID + "\",\"username\":\"" + USERNAME + "\",\"email\":\"" + EMAIL
                            + "\",\"attributes\":{\"" + USER_ID_ATTRIBUTE + "\":[\"" + ID_USUARIO + "\"]}}]");
            server.stubNoContent("PUT", RESET_PASSWORD_PATH, 204);
            stubRoleNoAsignado();

            final CuentaIdentidadDTO resultado = adapter.crearCuenta(nuevoUsuarioDto());

            assertTrue(resultado.newlyCreated());
            assertEquals(EXTERNAL_ID, resultado.idExterno());
            assertTrue(server.wasCalled("GET", SEARCH_PATH));
            assertTrue(server.wasCalled("PUT", RESET_PASSWORD_PATH));
        }

        @Test
        void ausencia_de_location_header_en_201_sin_resultado_falla_sin_delete() {
            server.stub("POST", USERS_PATH, 201, "", Map.of());
            server.stubJson("GET", SEARCH_PATH, 200, "[]");

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));

            assertEquals(0, server.callCount("DELETE", DELETE_PATH));
        }

        @Test
        void ausencia_de_location_header_en_201_con_identidad_inconsistente_falla_sin_delete() {
            server.stub("POST", USERS_PATH, 201, "", Map.of());
            server.stubFirstJson("GET", SEARCH_PATH, 200, "[]");
            server.stubJson("GET", SEARCH_PATH, 200,
                    "[{\"id\":\"" + EXTERNAL_ID + "\",\"username\":\"" + USERNAME + "\",\"email\":\"otro@uco.edu.co\""
                            + ",\"attributes\":{\"" + USER_ID_ATTRIBUTE + "\":[\"" + ID_USUARIO + "\"]}}]");

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));

            assertEquals(0, server.callCount("DELETE", DELETE_PATH));
        }

        @Test
        void ausencia_de_location_header_en_201_con_external_id_vacio_falla_sin_delete() {
            server.stub("POST", USERS_PATH, 201, "", Map.of());
            server.stubFirstJson("GET", SEARCH_PATH, 200, "[]");
            server.stubJson("GET", SEARCH_PATH, 200,
                    "[{\"id\":\"   \",\"username\":\"" + USERNAME + "\",\"email\":\"" + EMAIL
                            + "\",\"attributes\":{\"" + USER_ID_ATTRIBUTE + "\":[\"" + ID_USUARIO + "\"]}}]");

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));

            assertEquals(0, server.callCount("DELETE", DELETE_PATH));
        }

        @Test
        void externalId_vacio_desde_location_lanza_excepcion_sin_construir_operaciones_de_usuario() {
            server.stubCreated("POST", USERS_PATH, server.baseUrl() + USERS_PATH + "/");

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));

            assertFalse(server.wasCalled("PUT", RESET_PASSWORD_PATH));
            assertFalse(server.wasCalled("POST", ASSIGN_ROLE_PATH));
        }
    }

    // -------------------------------------------------------------------------
    // Bloque 37: conflicto 409 — nunca asumir éxito
    // -------------------------------------------------------------------------

    @Nested
    class ConflictTests {

        @Test
        void conflicto_409_con_cuenta_exacta_retorna_newlyCreated_false() {
            stubConflicto409ConExistente(USERNAME, EMAIL, ID_USUARIO.toString());
            stubRoleNoAsignado();

            final CuentaIdentidadDTO resultado = adapter.crearCuenta(nuevoUsuarioDto());

            assertFalse(resultado.newlyCreated());
            assertEquals(EXTERNAL_ID, resultado.idExterno());
        }

        @Test
        void conflicto_409_username_diferente_lanza_excepcion() {
            stubConflicto409ConExistente("otro-usuario", EMAIL, ID_USUARIO.toString());

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));
        }

        @Test
        void conflicto_409_email_diferente_lanza_excepcion() {
            stubConflicto409ConExistente(USERNAME, "otro@correo.com", ID_USUARIO.toString());

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));
        }

        @Test
        void conflicto_409_idUsuario_diferente_lanza_excepcion() {
            stubConflicto409ConExistente(USERNAME, EMAIL, UUID.randomUUID().toString());

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));
        }

        @Test
        void conflicto_409_idUsuario_con_varios_valores_lanza_excepcion() {
            server.stubNoContent("POST", USERS_PATH, 409);
            server.stubFirstJson("GET", SEARCH_PATH, 200, "[]");
            server.stubJson("GET", SEARCH_PATH, 200,
                    "[{\"id\":\"" + EXTERNAL_ID + "\",\"username\":\"" + USERNAME + "\",\"email\":\"" + EMAIL
                            + "\",\"attributes\":{\"" + USER_ID_ATTRIBUTE + "\":[\"" + ID_USUARIO + "\",\"OTRO-VALOR\"]}}]");

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));
            assertEquals(0, server.callCount("DELETE", DELETE_PATH));
        }

        @Test
        void mas_de_una_identidad_ambigua_lanza_excepcion() {
            server.stubNoContent("POST", USERS_PATH, 409);
            server.stubFirstJson("GET", SEARCH_PATH, 200, "[]");
            server.stubJson("GET", SEARCH_PATH, 200,
                    "[{\"id\":\"a\",\"username\":\"" + USERNAME + "\",\"email\":\"" + EMAIL + "\",\"attributes\":{}},"
                            + "{\"id\":\"b\",\"username\":\"" + USERNAME + "\",\"email\":\"" + EMAIL + "\",\"attributes\":{}}]");

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));
        }

        @Test
        void busqueda_de_existente_usa_atributo_institucional_codificado() {
            stubConflicto409ConExistente(USERNAME, EMAIL, ID_USUARIO.toString());
            stubRoleNoAsignado();

            adapter.crearCuenta(nuevoUsuarioDto());

            assertTrue(server.wasCalled("GET", SEARCH_PATH));
            assertTrue(SEARCH_PATH.contains("q=" + USER_ID_ATTRIBUTE + "%3A" + ID_USUARIO));
        }

        @Test
        void cuenta_existente_sin_externalId_valido_lanza_excepcion() {
            server.stubNoContent("POST", USERS_PATH, 409);
            server.stubFirstJson("GET", SEARCH_PATH, 200, "[]");
            server.stubJson("GET", SEARCH_PATH, 200,
                    "[{\"id\":\"   \",\"username\":\"" + USERNAME + "\",\"email\":\"" + EMAIL
                            + "\",\"attributes\":{\"" + USER_ID_ATTRIBUTE + "\":[\"" + ID_USUARIO + "\"]}}]");

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));
        }
    }

    // -------------------------------------------------------------------------
    // Bloque 38: password solo para usuario nuevo
    // -------------------------------------------------------------------------

    @Nested
    class PasswordTests {

        @Test
        void usuario_nuevo_establece_password_con_temporary_false() {
            stubCreacionExitosaCompleta();

            adapter.crearCuenta(nuevoUsuarioDto());

            final String body = server.lastRequestBody("PUT", RESET_PASSWORD_PATH);
            assertNotNull(body);
            assertTrue(body.contains("\"temporary\":false"));
        }

        @Test
        void usuario_existente_no_modifica_password() {
            stubConflicto409ConExistente(USERNAME, EMAIL, ID_USUARIO.toString());
            stubRoleNoAsignado();

            adapter.crearCuenta(nuevoUsuarioDto());

            assertFalse(server.wasCalled("PUT", RESET_PASSWORD_PATH));
        }

        @Test
        void fallo_password_en_usuario_nuevo_dispara_compensacion_delete() {
            stubCreacionHastaPassword();
            server.stub("PUT", RESET_PASSWORD_PATH, 400, "", Map.of());
            server.stubNoContent("DELETE", DELETE_PATH, 204);

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));

            assertTrue(server.wasCalled("DELETE", DELETE_PATH));
            assertEquals(1, server.callCount("POST", TOKEN_PATH));
            assertEquals("Bearer fake-admin-token", server.lastAuthorizationHeader("DELETE", DELETE_PATH));
        }
    }

    // -------------------------------------------------------------------------
    // Bloque 39: client role mapping
    // -------------------------------------------------------------------------

    @Nested
    class RoleMappingTests {

        @Test
        void api_client_ausente_lanza_excepcion_y_compensa() {
            stubCreacionHastaPassword();
            server.stubJson("GET", CLIENTS_SEARCH_PATH, 200, "[]");
            server.stubNoContent("DELETE", DELETE_PATH, 204);

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));

            assertTrue(server.wasCalled("DELETE", DELETE_PATH));
        }

        @Test
        void api_client_ambiguo_lanza_excepcion_y_compensa() {
            stubCreacionHastaPassword();
            server.stubJson("GET", CLIENTS_SEARCH_PATH, 200,
                    "[{\"id\":\"c1\",\"clientId\":\"" + API_CLIENT_ID + "\"},{\"id\":\"c2\",\"clientId\":\"" + API_CLIENT_ID + "\"}]");
            server.stubNoContent("DELETE", DELETE_PATH, 204);

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));

            assertTrue(server.wasCalled("DELETE", DELETE_PATH));
        }

        @Test
        void api_client_con_id_interno_vacio_lanza_excepcion_y_compensa() {
            stubCreacionHastaPassword();
            server.stubJson("GET", CLIENTS_SEARCH_PATH, 200,
                    "[{\"id\":\"   \",\"clientId\":\"" + API_CLIENT_ID + "\"}]");
            server.stubNoContent("DELETE", DELETE_PATH, 204);

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));

            assertTrue(server.wasCalled("DELETE", DELETE_PATH));
        }

        @Test
        void api_client_distinto_al_solicitado_lanza_excepcion_y_compensa() {
            stubCreacionHastaPassword();
            server.stubJson("GET", CLIENTS_SEARCH_PATH, 200,
                    "[{\"id\":\"" + CLIENT_UUID + "\",\"clientId\":\"otro-client\"}]");
            server.stubNoContent("DELETE", DELETE_PATH, 204);

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));

            assertTrue(server.wasCalled("DELETE", DELETE_PATH));
        }

        @Test
        void role_ausente_lanza_excepcion_y_compensa() {
            stubCreacionHastaPassword();
            server.stubJson("GET", CLIENTS_SEARCH_PATH, 200,
                    "[{\"id\":\"" + CLIENT_UUID + "\",\"clientId\":\"" + API_CLIENT_ID + "\"}]");
            server.stub("GET", ROLE_PATH, 404, "", Map.of());
            server.stubNoContent("DELETE", DELETE_PATH, 204);

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));

            assertTrue(server.wasCalled("DELETE", DELETE_PATH));
        }

        @Test
        void role_con_id_vacio_lanza_excepcion_y_compensa() {
            stubCreacionHastaPassword();
            server.stubJson("GET", CLIENTS_SEARCH_PATH, 200,
                    "[{\"id\":\"" + CLIENT_UUID + "\",\"clientId\":\"" + API_CLIENT_ID + "\"}]");
            server.stubJson("GET", ROLE_PATH, 200,
                    "{\"id\":\"   \",\"name\":\"" + ROLE_NAME + "\",\"clientRole\":true,\"containerId\":\"" + CLIENT_UUID + "\"}");
            server.stubNoContent("DELETE", DELETE_PATH, 204);

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));

            assertTrue(server.wasCalled("DELETE", DELETE_PATH));
        }

        @Test
        void role_con_nombre_distinto_lanza_excepcion_y_compensa() {
            stubCreacionHastaPassword();
            server.stubJson("GET", CLIENTS_SEARCH_PATH, 200,
                    "[{\"id\":\"" + CLIENT_UUID + "\",\"clientId\":\"" + API_CLIENT_ID + "\"}]");
            server.stubJson("GET", ROLE_PATH, 200,
                    "{\"id\":\"role-1\",\"name\":\"ESTUDIANTE\",\"clientRole\":true,\"containerId\":\"" + CLIENT_UUID + "\"}");
            server.stubNoContent("DELETE", DELETE_PATH, 204);

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));

            assertTrue(server.wasCalled("DELETE", DELETE_PATH));
        }

        @Test
        void role_encontrado_usa_exactamente_institutional_role_name_sin_alias_ni_prefijo() {
            stubCreacionExitosaCompleta();

            adapter.crearCuenta(nuevoUsuarioDto());

            assertTrue(server.wasCalled("GET", ROLE_PATH));
            assertTrue(ROLE_PATH.endsWith("/roles/" + ROLE_NAME));
            assertFalse(ROLE_PATH.contains("ROLE_"));
        }

        @Test
        void role_assignment_correcto_reenvia_representacion_exacta_obtenida_de_keycloak() {
            stubCreacionExitosaCompleta();

            adapter.crearCuenta(nuevoUsuarioDto());

            final String assignedBody = server.lastRequestBody("POST", ASSIGN_ROLE_PATH);
            assertNotNull(assignedBody);
            assertTrue(assignedBody.contains("\"id\":\"role-1\""));
            assertTrue(assignedBody.contains("\"name\":\"" + ROLE_NAME + "\""));
        }

        @Test
        void role_assignment_falla_lanza_excepcion_y_compensa() {
            stubCreacionHastaPassword();
            server.stubJson("GET", CLIENTS_SEARCH_PATH, 200,
                    "[{\"id\":\"" + CLIENT_UUID + "\",\"clientId\":\"" + API_CLIENT_ID + "\"}]");
            server.stubJson("GET", ROLE_PATH, 200,
                    "{\"id\":\"role-1\",\"name\":\"" + ROLE_NAME + "\",\"clientRole\":true,\"containerId\":\"" + CLIENT_UUID + "\"}");
            server.stubJson("GET", ROLE_MAPPINGS_PATH, 200, "[]");
            server.stub("POST", ASSIGN_ROLE_PATH, 400, "", Map.of());
            server.stubNoContent("DELETE", DELETE_PATH, 204);

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));

            assertTrue(server.wasCalled("DELETE", DELETE_PATH));
        }

        @Test
        void cuenta_existente_sin_rol_asigna_client_role_sin_password_ni_delete() {
            stubConflicto409ConExistente(USERNAME, EMAIL, ID_USUARIO.toString());
            stubRoleNoAsignado();

            final CuentaIdentidadDTO resultado = adapter.crearCuenta(nuevoUsuarioDto());

            assertFalse(resultado.newlyCreated());
            assertTrue(server.wasCalled("GET", ROLE_MAPPINGS_PATH));
            assertEquals(1, server.callCount("POST", ASSIGN_ROLE_PATH));
            assertFalse(server.wasCalled("PUT", RESET_PASSWORD_PATH));
            assertEquals(0, server.callCount("DELETE", DELETE_PATH));
        }

        @Test
        void cuenta_existente_con_rol_ya_asignado_no_duplica_mapping() {
            stubConflicto409ConExistente(USERNAME, EMAIL, ID_USUARIO.toString());
            stubRoleYaAsignado();

            final CuentaIdentidadDTO resultado = adapter.crearCuenta(nuevoUsuarioDto());

            assertFalse(resultado.newlyCreated());
            assertTrue(server.wasCalled("GET", ROLE_MAPPINGS_PATH));
            assertEquals(0, server.callCount("POST", ASSIGN_ROLE_PATH));
        }

        @Test
        void cuenta_existente_con_fallo_de_role_lanza_excepcion_sin_password_ni_delete() {
            stubConflicto409ConExistente(USERNAME, EMAIL, ID_USUARIO.toString());
            server.stubJson("GET", CLIENTS_SEARCH_PATH, 200,
                    "[{\"id\":\"" + CLIENT_UUID + "\",\"clientId\":\"" + API_CLIENT_ID + "\"}]");
            server.stubJson("GET", ROLE_PATH, 200,
                    "{\"id\":\"role-1\",\"name\":\"" + ROLE_NAME + "\",\"clientRole\":true,\"containerId\":\"" + CLIENT_UUID + "\"}");
            server.stub("GET", ROLE_MAPPINGS_PATH, 500, "", Map.of());

            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.crearCuenta(nuevoUsuarioDto()));

            assertFalse(server.wasCalled("PUT", RESET_PASSWORD_PATH));
            assertEquals(0, server.callCount("DELETE", DELETE_PATH));
        }
    }

    // -------------------------------------------------------------------------
    // Bloque 40: compensación
    // -------------------------------------------------------------------------

    @Nested
    class CompensationTests {

        @Test
        void usuario_existente_con_conflicto_nunca_dispara_delete() {
            stubConflicto409ConExistente(USERNAME, EMAIL, ID_USUARIO.toString());
            stubRoleNoAsignado();

            adapter.crearCuenta(nuevoUsuarioDto());

            assertEquals(0, server.callCount("DELETE", DELETE_PATH));
        }

        @Test
        void fallo_provisioning_y_fallo_delete_conserva_el_error_principal() {
            stubCreacionHastaPassword();
            server.stub("PUT", RESET_PASSWORD_PATH, 400, "", Map.of());
            server.stub("DELETE", DELETE_PATH, 500, "", Map.of());

            final IdentityProviderPort.IdentityProviderException exception = assertThrows(
                    IdentityProviderPort.IdentityProviderException.class,
                    () -> adapter.crearCuenta(nuevoUsuarioDto())
            );

            assertTrue(exception.getMessage().toLowerCase(Locale.ROOT).contains("contrasena"));
        }

        @Test
        void eliminarCuenta_publico_es_best_effort_y_nunca_lanza() {
            server.stub("DELETE", DELETE_PATH, 500, "", Map.of());

            assertDoesNotThrow(() -> adapter.eliminarCuenta(EXTERNAL_ID));
        }

        @Test
        void eliminarCuenta_con_idExterno_vacio_es_ignorada_sin_llamar_a_keycloak() {
            adapter.eliminarCuenta("");

            assertEquals(0, server.callCount("POST", TOKEN_PATH));
        }
    }

    // -------------------------------------------------------------------------
    // Bloque 41: asignarRol — rol institucional tipado, nunca String libre
    // -------------------------------------------------------------------------

    @Nested
    class AssignRoleTests {

        @Test
        void asigna_client_role_cuando_no_esta_presente() {
            stubRoleNoAsignado();

            adapter.asignarRol(EXTERNAL_ID, ROLE);

            assertTrue(server.wasCalled("GET", ROLE_PATH));
            assertEquals(1, server.callCount("POST", ASSIGN_ROLE_PATH));
        }

        @Test
        void es_idempotente_cuando_el_role_ya_esta_asignado() {
            stubRoleYaAsignado();

            adapter.asignarRol(EXTERNAL_ID, ROLE);

            assertTrue(server.wasCalled("GET", ROLE_MAPPINGS_PATH));
            assertEquals(0, server.callCount("POST", ASSIGN_ROLE_PATH));
        }

        @Test
        void traduce_el_enum_institucional_al_nombre_exacto_del_client_role_sin_prefijo_role() {
            stubRoleNoAsignado();

            adapter.asignarRol(EXTERNAL_ID, ROLE);

            assertTrue(ROLE_PATH.endsWith("/roles/" + ROLE_NAME));
            assertFalse(ROLE_PATH.contains("ROLE_"));
        }

        @Test
        void rol_nulo_lanza_excepcion_sin_llamar_a_keycloak() {
            assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.asignarRol(EXTERNAL_ID, null));

            assertEquals(0, server.callCount("POST", TOKEN_PATH));
        }
    }
}
