package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.identity;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adaptador que implementa {@link IdentityProviderPort} usando la API de administración de Keycloak.
 *
 * <p>Utiliza {@code java.net.http.HttpClient} nativo de Java y construcción/parseo de JSON
 * con la API estándar — sin SDKs de Keycloak ni dependencias externas adicionales —
 * para mantener el módulo independiente y facilitar la migración futura a otro IdP.</p>
 *
 * <p>Para reemplazar Keycloak, basta con crear otro adaptador que implemente
 * {@link IdentityProviderPort} y registrarlo en {@code IdentityProviderBeansConfig}.</p>
 */
public final class KeycloakIdentityProviderAdapter implements IdentityProviderPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakIdentityProviderAdapter.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    /** Extrae el valor de un campo JSON simple: "campo":"valor" */
    private static final Pattern JSON_STRING_FIELD = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"");
    /** Extrae el "id" de un primer objeto de un array JSON: [{"id":"...","...} */
    private static final Pattern JSON_ARRAY_FIRST_ID = Pattern.compile("\\[\\s*\\{[^}]*\"id\"\\s*:\\s*\"([^\"]+)\"");

    private final String serverUrl;
    private final String realm;
    private final String adminUsername;
    private final String adminPassword;
    private final String clientId;
    private final HttpClient httpClient;

    public KeycloakIdentityProviderAdapter(
            final String serverUrl,
            final String realm,
            final String adminUsername,
            final String adminPassword,
            final String clientId
    ) {
        this.serverUrl = Objects.requireNonNull(serverUrl, "Keycloak serverUrl es obligatorio");
        this.realm = Objects.requireNonNull(realm, "Keycloak realm es obligatorio");
        this.adminUsername = Objects.requireNonNull(adminUsername, "Keycloak adminUsername es obligatorio");
        this.adminPassword = Objects.requireNonNull(adminPassword, "Keycloak adminPassword es obligatorio");
        this.clientId = Objects.requireNonNull(clientId, "Keycloak clientId es obligatorio");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    // -------------------------------------------------------------------------
    // Puerto principal
    // -------------------------------------------------------------------------

    @Override
    public CuentaIdentidadDTO crearCuenta(final CrearCuentaIdentidadDTO dto) {
        Objects.requireNonNull(dto, "El DTO para crear cuenta es obligatorio");
        LOGGER.info("Creando cuenta en Keycloak para username={}, rol={}", dto.username(), dto.rolInstitucional());

        final String adminToken = obtenerAdminToken();

        // 1. Crear usuario en Keycloak
        final String keycloakUserId = crearUsuarioEnKeycloak(adminToken, dto);
        LOGGER.info("Usuario creado en Keycloak con id={}", keycloakUserId);

        // 2. Establecer contraseña inicial
        establecerPassword(adminToken, keycloakUserId, dto.passwordInicial());

        // 3. Asignar rol institucional
        asignarRolInterno(adminToken, keycloakUserId, dto.rolInstitucional());

        return new CuentaIdentidadDTO(
                keycloakUserId,
                dto.username(),
                "Cuenta creada exitosamente en el proveedor de identidad con rol " + dto.rolInstitucional()
        );
    }

    @Override
    public void eliminarCuenta(final String idExterno) {
        if (idExterno == null || idExterno.isBlank()) {
            LOGGER.warn("Se intentó eliminar una cuenta con idExterno nulo o vacío — operación ignorada");
            return;
        }
        LOGGER.info("Eliminando cuenta de compensación en Keycloak para idExterno={}", idExterno);
        try {
            final String adminToken = obtenerAdminToken();
            final String url = serverUrl + "/admin/realms/" + realm + "/users/" + idExterno;
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + adminToken)
                    .DELETE()
                    .timeout(TIMEOUT)
                    .build();
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 204 && response.statusCode() != 404) {
                LOGGER.warn("No se pudo eliminar cuenta de compensación en Keycloak, idExterno={}, status={}",
                        idExterno, response.statusCode());
            }
        } catch (Exception ex) {
            LOGGER.error("Error de compensación al eliminar cuenta en Keycloak idExterno={}", idExterno, ex);
        }
    }

    @Override
    public void asignarRol(final String idExterno, final String nombreRol) {
        Objects.requireNonNull(idExterno, "idExterno es obligatorio para asignar rol");
        Objects.requireNonNull(nombreRol, "nombreRol es obligatorio");
        final String adminToken = obtenerAdminToken();
        asignarRolInterno(adminToken, idExterno, nombreRol);
    }

    // -------------------------------------------------------------------------
    // Operaciones internas de Keycloak
    // -------------------------------------------------------------------------

    /**
     * Obtiene un access token de administración usando las credenciales del cliente admin-cli.
     */
    private String obtenerAdminToken() {
        try {
            final String tokenUrl = serverUrl + "/realms/master/protocol/openid-connect/token";
            final String formBody = "grant_type=password"
                    + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                    + "&username=" + URLEncoder.encode(adminUsername, StandardCharsets.UTF_8)
                    + "&password=" + URLEncoder.encode(adminPassword, StandardCharsets.UTF_8);

            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .timeout(TIMEOUT)
                    .build();

            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IdentityProviderException(
                        "No se pudo obtener token de administración de Keycloak. HTTP " + response.statusCode());
            }

            return extraerCampoJson(response.body(), "access_token")
                    .orElseThrow(() -> new IdentityProviderException("Token de admin no encontrado en respuesta de Keycloak"));
        } catch (IdentityProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IdentityProviderException("Error al comunicarse con Keycloak para obtener token de admin", ex);
        }
    }

    /**
     * Crea el usuario en Keycloak y retorna su ID interno.
     * Si el usuario ya existe (HTTP 409), lo busca y retorna su ID existente.
     */
    private String crearUsuarioEnKeycloak(final String adminToken, final CrearCuentaIdentidadDTO dto) {
        try {
            final String usersUrl = serverUrl + "/admin/realms/" + realm + "/users";
            final String userJson = buildUserJson(dto);

            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(usersUrl))
                    .header("Authorization", "Bearer " + adminToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(userJson))
                    .timeout(TIMEOUT)
                    .build();

            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                // La ubicación del nuevo usuario viene en el header Location
                final String location = response.headers().firstValue("Location").orElse("");
                return location.substring(location.lastIndexOf('/') + 1);
            }

            if (response.statusCode() == 409) {
                // El usuario ya existe — buscarlo por username
                LOGGER.warn("Usuario con username={} ya existe en Keycloak, recuperando su ID", dto.username());
                return buscarUsuarioPorUsername(adminToken, dto.username());
            }

            throw new IdentityProviderException(
                    "Error al crear usuario en Keycloak. HTTP " + response.statusCode() + ": " + response.body());
        } catch (IdentityProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IdentityProviderException("Error de comunicación con Keycloak al crear usuario", ex);
        }
    }

    /**
     * Establece la contraseña inicial del usuario en Keycloak (temporal = false).
     */
    private void establecerPassword(final String adminToken, final String keycloakUserId, final String password) {
        try {
            final String passwordUrl = serverUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId + "/reset-password";
            final String credJson = "{\"type\":\"password\",\"value\":" + jsonString(password) + ",\"temporary\":false}";

            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(passwordUrl))
                    .header("Authorization", "Bearer " + adminToken)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(credJson))
                    .timeout(TIMEOUT)
                    .build();

            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 204) {
                LOGGER.warn("No se pudo establecer contraseña en Keycloak para userId={}, status={}",
                        keycloakUserId, response.statusCode());
            }
        } catch (Exception ex) {
            LOGGER.warn("Error al establecer contraseña en Keycloak para userId={}", keycloakUserId, ex);
        }
    }

    /**
     * Busca un usuario por username en Keycloak y retorna su ID.
     */
    private String buscarUsuarioPorUsername(final String adminToken, final String username) {
        try {
            final String searchUrl = serverUrl + "/admin/realms/" + realm + "/users?username="
                    + URLEncoder.encode(username, StandardCharsets.UTF_8) + "&exact=true";

            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(searchUrl))
                    .header("Authorization", "Bearer " + adminToken)
                    .GET()
                    .timeout(TIMEOUT)
                    .build();

            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            final Matcher m = JSON_ARRAY_FIRST_ID.matcher(response.body());
            if (m.find()) {
                return m.group(1);
            }
            throw new IdentityProviderException("Usuario con username=" + username + " no encontrado en Keycloak");
        } catch (IdentityProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IdentityProviderException("Error al buscar usuario en Keycloak por username=" + username, ex);
        }
    }

    /**
     * Obtiene la representación de un rol de realm por nombre y lo asigna al usuario.
     */
    private void asignarRolInterno(final String adminToken, final String keycloakUserId, final String nombreRol) {
        try {
            // 1. Obtener representación del rol
            final String rolUrl = serverUrl + "/admin/realms/" + realm + "/roles/" + nombreRol;
            final HttpRequest getRolRequest = HttpRequest.newBuilder()
                    .uri(URI.create(rolUrl))
                    .header("Authorization", "Bearer " + adminToken)
                    .GET()
                    .timeout(TIMEOUT)
                    .build();

            final HttpResponse<String> rolResponse = httpClient.send(getRolRequest, HttpResponse.BodyHandlers.ofString());
            if (rolResponse.statusCode() != 200) {
                LOGGER.warn("Rol '{}' no encontrado en Keycloak realm '{}'. El usuario fue creado sin rol.", nombreRol, realm);
                return;
            }

            // 2. Asignar el rol al usuario — el body debe ser un array con el objeto del rol tal cual
            final String assignUrl = serverUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId + "/role-mappings/realm";
            final String roleArrayJson = "[" + rolResponse.body() + "]";

            final HttpRequest assignRequest = HttpRequest.newBuilder()
                    .uri(URI.create(assignUrl))
                    .header("Authorization", "Bearer " + adminToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(roleArrayJson))
                    .timeout(TIMEOUT)
                    .build();

            final HttpResponse<String> assignResponse = httpClient.send(assignRequest, HttpResponse.BodyHandlers.ofString());
            if (assignResponse.statusCode() != 204) {
                LOGGER.warn("No se pudo asignar rol '{}' al usuario '{}' en Keycloak, status={}",
                        nombreRol, keycloakUserId, assignResponse.statusCode());
            } else {
                LOGGER.info("Rol '{}' asignado exitosamente al usuario '{}' en Keycloak", nombreRol, keycloakUserId);
            }
        } catch (Exception ex) {
            LOGGER.warn("Error al asignar rol '{}' al usuario '{}' en Keycloak", nombreRol, keycloakUserId, ex);
        }
    }

    // -------------------------------------------------------------------------
    // Utilidades de JSON sin dependencias externas
    // -------------------------------------------------------------------------

    /**
     * Construye el JSON de representación de usuario de Keycloak.
     */
    private static String buildUserJson(final CrearCuentaIdentidadDTO dto) {
        return "{" +
                "\"username\":" + jsonString(dto.username()) + "," +
                "\"email\":" + jsonString(dto.correo()) + "," +
                "\"firstName\":" + jsonString(dto.primerNombre()) + "," +
                "\"lastName\":" + jsonString(dto.primerApellido()) + "," +
                "\"enabled\":true," +
                "\"emailVerified\":true" +
                "}";
    }

    /**
     * Escapa un valor String para incluirlo de forma segura en JSON.
     */
    private static String jsonString(final String value) {
        if (value == null) return "null";
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    /**
     * Extrae el valor de un campo simple de un objeto JSON plano usando regex.
     * Apto para respuestas JSON simples como tokens y representaciones de roles.
     */
    private static java.util.Optional<String> extraerCampoJson(final String json, final String campo) {
        final Pattern p = Pattern.compile("\"" + Pattern.quote(campo) + "\"\\s*:\\s*\"([^\"]+)\"");
        final Matcher m = p.matcher(json);
        return m.find() ? java.util.Optional.of(m.group(1)) : java.util.Optional.empty();
    }
}
