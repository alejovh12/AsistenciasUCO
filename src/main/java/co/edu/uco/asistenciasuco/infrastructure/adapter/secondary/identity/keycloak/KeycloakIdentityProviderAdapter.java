package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.identity.keycloak;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador que implementa {@link IdentityProviderPort} usando la Admin REST API de Keycloak,
 * exclusivamente para provisioning administrativo (crear/verificar/eliminar cuentas
 * institucionales y asignar client roles). No participa en runtime security — eso vive en
 * {@code KeycloakJwtClaimsExtractor} (ver {@code docs/security/runtime-security-provider-architecture.md}).
 *
 * <p>Usa {@code client_credentials} contra un client confidencial de service account
 * ({@code adminClientId}/{@code adminClientSecret}) — nunca password grant ni un
 * usuario/contraseña administrativo. Los roles institucionales se asignan como
 * <b>client roles</b> del client {@code apiClientId} (nunca realm roles), porque eso es
 * exactamente lo que runtime security lee de {@code resource_access[apiClientId].roles}.</p>
 *
 * <p>Utiliza {@code java.net.http.HttpClient} nativo de Java y Jackson para todo el JSON
 * (nunca regex ni concatenación manual) — sin SDKs de Keycloak ni dependencias adicionales —
 * para mantener el módulo independiente y facilitar la migración futura a otro IdP.</p>
 *
 * <p>Para reemplazar Keycloak, basta con crear otro adaptador que implemente
 * {@link IdentityProviderPort} y una configuración equivalente a
 * {@code KeycloakIdentityAdapterConfiguration}, condicionada a
 * {@code app.adapters.identity.provider}. No requiere cambios en Application ni Domain.</p>
 */
public final class KeycloakIdentityProviderAdapter implements IdentityProviderPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakIdentityProviderAdapter.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    private static final String PROVIDER_NAME = "keycloak";

    private final String serverUrl;
    private final String realm;
    private final String adminClientId;
    private final String adminClientSecret;
    private final String apiClientId;
    private final String userIdAttribute;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public KeycloakIdentityProviderAdapter(
            final String serverUrl,
            final String realm,
            final String adminClientId,
            final String adminClientSecret,
            final String apiClientId,
            final String userIdAttribute
    ) {
        this.serverUrl = Objects.requireNonNull(serverUrl, "Keycloak serverUrl es obligatorio");
        this.realm = Objects.requireNonNull(realm, "Keycloak realm es obligatorio");
        this.adminClientId = Objects.requireNonNull(adminClientId, "Keycloak adminClientId es obligatorio");
        this.adminClientSecret = Objects.requireNonNull(adminClientSecret, "Keycloak adminClientSecret es obligatorio");
        this.apiClientId = Objects.requireNonNull(apiClientId, "Keycloak apiClientId es obligatorio");
        this.userIdAttribute = Objects.requireNonNull(userIdAttribute, "Keycloak userIdAttribute es obligatorio");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    // -------------------------------------------------------------------------
    // Puerto principal
    // -------------------------------------------------------------------------

    @Override
    public CuentaIdentidadDTO crearCuenta(final CrearCuentaIdentidadDTO dto) {
        Objects.requireNonNull(dto, "El DTO para crear cuenta es obligatorio");
        Objects.requireNonNull(dto.idUsuario(), "idUsuario es obligatorio para crear una identidad institucional");
        final String institutionalRoleName = requireValidInstitutionalRoleName(dto.rolInstitucional());

        LOGGER.atInfo()
                .addKeyValue("operation", "IDENTITY_CREATE_ACCOUNT")
                .addKeyValue("stage", "START")
                .addKeyValue("realm", realm)
                .log("Iniciando provisioning de identidad institucional en Keycloak.");

        final String adminToken = obtenerAdminToken();
        final UserResolution resolution = resolverUsuario(adminToken, dto);

        try {
            if (resolution.newlyCreated()) {
                establecerPassword(adminToken, resolution.externalId(), dto.passwordInicial());
            }
            ensureClientRole(adminToken, resolution.externalId(), institutionalRoleName);
        } catch (final IdentityProviderException exception) {
            if (resolution.newlyCreated()) {
                // newlyCreated=true es la UNICA condicion bajo la cual se compensa con DELETE.
                // Se reutiliza el adminToken obtenido al inicio de esta misma operacion.
                deleteUserBestEffort(adminToken, resolution.externalId());
            }
            throw exception;
        }

        LOGGER.atInfo()
                .addKeyValue("operation", "IDENTITY_CREATE_ACCOUNT")
                .addKeyValue("stage", "COMPLETED")
                .addKeyValue("realm", realm)
                .addKeyValue("externalUserId", resolution.externalId())
                .addKeyValue("newlyCreated", resolution.newlyCreated())
                .addKeyValue("institutionalRole", institutionalRoleName)
                .log("Identidad institucional resuelta en Keycloak.");

        return new CuentaIdentidadDTO(resolution.externalId(), resolution.newlyCreated());
    }

    /**
     * Elimina la cuenta en Keycloak. Best-effort: nunca propaga excepción — un fallo aquí no
     * debe ocultar el fallo principal que originó la compensación (ver {@link #crearCuenta}).
     */
    @Override
    public void eliminarCuenta(final String idExterno) {
        if (idExterno == null || idExterno.isBlank()) {
            LOGGER.warn("Se intento eliminar una cuenta con idExterno nulo o vacio - operacion ignorada.");
            return;
        }
        LOGGER.atInfo()
                .addKeyValue("operation", "IDENTITY_DELETE_ACCOUNT")
                .addKeyValue("realm", realm)
                .addKeyValue("externalUserId", idExterno)
                .log("Eliminando cuenta de compensacion en Keycloak.");
        try {
            final String adminToken = obtenerAdminToken();
            deleteUser(adminToken, idExterno);
        } catch (final Exception exception) {
            LOGGER.atError()
                    .addKeyValue("eventType", "IDENTITY_COMPENSATION_FAILED")
                    .addKeyValue("correlationId", CorrelationIdContext.getAsString())
                    .addKeyValue("externalUserId", idExterno)
                    .addKeyValue("provider", PROVIDER_NAME)
                    .setCause(exception)
                    .log("No fue posible eliminar la cuenta de compensacion en Keycloak.");
        }
    }

    @Override
    public void asignarRol(final String idExterno, final InstitutionalRole rol) {
        Objects.requireNonNull(idExterno, "idExterno es obligatorio para asignar rol");
        final String institutionalRoleName = requireValidInstitutionalRoleName(rol);
        final String adminToken = obtenerAdminToken();
        ensureClientRole(adminToken, idExterno, institutionalRoleName);
    }

    // -------------------------------------------------------------------------
    // Token administrativo (client_credentials — NUNCA password grant)
    // -------------------------------------------------------------------------

    private String obtenerAdminToken() {
        final String tokenUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        final String formBody = "grant_type=client_credentials"
                + "&client_id=" + encode(adminClientId)
                + "&client_secret=" + encode(adminClientSecret);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", CONTENT_TYPE_FORM)
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .timeout(TIMEOUT)
                .build();

        final HttpResponse<String> response = send(request);
        if (!isSuccess(response.statusCode())) {
            throw new IdentityProviderException(
                    "No se pudo obtener token de administracion de Keycloak (client_credentials). HTTP "
                            + response.statusCode());
        }

        final AdminTokenResponse tokenResponse = readJson(response.body(), AdminTokenResponse.class);
        final String accessToken = tokenResponse == null ? null : tokenResponse.accessToken();
        if (accessToken == null || accessToken.isBlank()) {
            throw new IdentityProviderException("Keycloak no devolvio un access_token valido para client_credentials.");
        }
        return accessToken;
    }

    // -------------------------------------------------------------------------
    // Resolución por vínculo institucional estable (creación + conflicto 409)
    // -------------------------------------------------------------------------

    private UserResolution resolverUsuario(final String adminToken, final CrearCuentaIdentidadDTO dto) {
        final Optional<String> existingExternalId = buscarUsuarioPorIdUsuarioVerificado(adminToken, dto);
        if (existingExternalId.isPresent()) {
            return new UserResolution(existingExternalId.get(), false);
        }

        final HttpResponse<String> response = crearUsuarioEnKeycloak(adminToken, dto);

        if (response.statusCode() == 201) {
            final String locationExternalId = resolverExternalIdDesdeLocation(response);
            final String externalId = locationExternalId != null
                    ? locationExternalId
                    : buscarUsuarioPorIdUsuarioVerificado(adminToken, dto).orElseThrow(() ->
                            new IdentityProviderException("No fue posible resolver la identidad creada en Keycloak."));
            LOGGER.atInfo()
                    .addKeyValue("operation", "IDENTITY_CREATE_ACCOUNT")
                    .addKeyValue("stage", "CREATED")
                    .addKeyValue("externalUserId", externalId)
                    .log("Usuario nuevo creado en Keycloak.");
            return new UserResolution(externalId, true);
        }

        if (response.statusCode() == 409) {
            LOGGER.atWarn()
                    .addKeyValue("operation", "IDENTITY_CREATE_ACCOUNT")
                    .addKeyValue("stage", "CONFLICT_VERIFYING_EXISTING")
                    .log("Keycloak reporto conflicto de usuario existente; verificando identidad exacta.");
            return new UserResolution(buscarUsuarioPorIdUsuarioVerificado(adminToken, dto).orElseThrow(() ->
                    new IdentityProviderException("El conflicto de identidad no pudo resolverse por vinculo institucional.")), false);
        }

        throw new IdentityProviderException("Error al crear usuario en Keycloak. HTTP " + response.statusCode());
    }

    private HttpResponse<String> crearUsuarioEnKeycloak(final String adminToken, final CrearCuentaIdentidadDTO dto) {
        final UserCreationRequest payload = new UserCreationRequest(
                dto.username(),
                dto.correo(),
                dto.primerNombre(),
                dto.primerApellido(),
                true,
                true,
                Map.of(userIdAttribute, List.of(dto.idUsuario().toString()))
        );
        final HttpRequest request = authorizedRequestBuilder(adminUrl("/users"), adminToken)
                .header("Content-Type", CONTENT_TYPE_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(writeJson(payload)))
                .build();
        return send(request);
    }

    /** Devuelve null si Location no identifica inequívocamente al usuario creado. */
    private String resolverExternalIdDesdeLocation(final HttpResponse<String> response) {
        final String location = response.headers().firstValue("Location").orElse(null);
        if (location == null || location.isBlank()) {
            return null;
        }
        try {
            final URI actual = URI.create(location);
            final URI expected = URI.create(adminUrl("/users/"));
            if (!Objects.equals(actual.getScheme(), expected.getScheme())
                    || !Objects.equals(actual.getAuthority(), expected.getAuthority())
                    || actual.getQuery() != null
                    || actual.getFragment() != null
                    || actual.getPath() == null
                    || !actual.getPath().startsWith(expected.getPath())) {
                return null;
            }
            final String externalId = actual.getPath().substring(expected.getPath().length());
            if (externalId.isBlank() || externalId.contains("/") || !externalId.equals(externalId.trim())) {
                return null;
            }
            return externalId;
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    /**
     * La consulta por atributo institucional puede devolver candidatos amplios; se exige un
     * resultado unico y coincidencia exacta de atributo, username y correo canonico.
     */
    private Optional<String> buscarUsuarioPorIdUsuarioVerificado(
            final String adminToken, final CrearCuentaIdentidadDTO dto
    ) {
        final String searchUrl = adminUrl("/users?q=" + encode(userIdAttribute + ":" + dto.idUsuario()));
        final HttpRequest request = authorizedRequestBuilder(searchUrl, adminToken).GET().build();
        final HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            throw new IdentityProviderException(
                    "No fue posible consultar usuarios existentes en Keycloak. HTTP " + response.statusCode());
        }

        final UserSearchResult[] users = readJson(response.body(), UserSearchResult[].class);
        if (users == null) {
            throw new IdentityProviderException("Keycloak devolvio una busqueda de identidad invalida.");
        }
        if (users.length == 0) {
            return Optional.empty();
        }
        if (users.length != 1) {
            throw new IdentityProviderException(
                    "La identidad institucional no pudo verificarse de forma inequivoca ("
                            + users.length + " resultados para atributo institucional).");
        }

        final UserSearchResult existing = users[0];
        verificarCoincidenciaExacta(existing, dto);
        return Optional.of(requireValidExternalUserId(
                existing.id(),
                "Keycloak devolvio una cuenta existente sin externalId valido."
        ));
    }

    private void verificarCoincidenciaExacta(final UserSearchResult existing, final CrearCuentaIdentidadDTO expected) {
        if (!Objects.equals(existing.username(), expected.username())) {
            throw new IdentityProviderException(
                    "La cuenta existente en Keycloak no coincide en username con el usuario institucional.");
        }
        if (!Objects.equals(existing.email(), expected.correo())) {
            throw new IdentityProviderException(
                    "La cuenta existente en Keycloak no coincide en email con el usuario institucional.");
        }
        final List<String> userIds = existing.attributes() == null
                ? null : existing.attributes().get(userIdAttribute);
        final String existingUserId = userIds != null && userIds.size() == 1 ? userIds.get(0) : null;
        if (!Objects.equals(existingUserId, expected.idUsuario().toString())) {
            throw new IdentityProviderException(
                    "La cuenta existente en Keycloak tiene un " + userIdAttribute
                            + " distinto al esperado; no se reutiliza.");
        }
    }

    // -------------------------------------------------------------------------
    // Password (solo para cuentas nuevas)
    // -------------------------------------------------------------------------

    private void establecerPassword(final String adminToken, final String externalId, final String password) {
        final String validatedExternalId = requireValidExternalUserId(
                externalId,
                "externalUserId es obligatorio para establecer password en Keycloak."
        );
        final CredentialRepresentation credential = new CredentialRepresentation("password", password, false);
        final HttpRequest request = authorizedRequestBuilder(
                adminUrl("/users/" + validatedExternalId + "/reset-password"), adminToken)
                .header("Content-Type", CONTENT_TYPE_JSON)
                .PUT(HttpRequest.BodyPublishers.ofString(writeJson(credential)))
                .build();
        final HttpResponse<String> response = send(request);
        if (response.statusCode() != 204) {
            throw new IdentityProviderException("No se pudo establecer contrasena en Keycloak. HTTP " + response.statusCode());
        }
    }

    // -------------------------------------------------------------------------
    // Client role (nunca realm role) — resolución de client + rol + asignación
    // -------------------------------------------------------------------------

    private void ensureClientRole(final String adminToken, final String externalId, final String roleName) {
        final String validatedExternalId = requireValidExternalUserId(
                externalId,
                "externalUserId es obligatorio para asegurar client role en Keycloak."
        );
        final String clientUuid = resolverApiClientUuid(adminToken);
        final KeycloakRoleRepresentation roleRepresentation = resolverClientRole(adminToken, clientUuid, roleName);
        if (clientRoleAlreadyAssigned(adminToken, validatedExternalId, clientUuid, roleName)) {
            LOGGER.atInfo()
                    .addKeyValue("operation", "IDENTITY_ASSIGN_CLIENT_ROLE")
                    .addKeyValue("stage", "ALREADY_ASSIGNED")
                    .addKeyValue("apiClientId", apiClientId)
                    .addKeyValue("externalUserId", validatedExternalId)
                    .log("Client role institucional ya estaba asignado en Keycloak.");
            return;
        }

        asignarClientRoleRepresentation(adminToken, validatedExternalId, clientUuid, roleRepresentation);
        LOGGER.atInfo()
                .addKeyValue("operation", "IDENTITY_ASSIGN_CLIENT_ROLE")
                .addKeyValue("apiClientId", apiClientId)
                .addKeyValue("externalUserId", validatedExternalId)
                .log("Client role institucional asignado en Keycloak.");
    }

    /** Resuelve el UUID interno del client de la API — nunca hardcodeado. */
    private String resolverApiClientUuid(final String adminToken) {
        final String url = adminUrl("/clients?clientId=" + encode(apiClientId));
        final HttpRequest request = authorizedRequestBuilder(url, adminToken).GET().build();
        final HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            throw new IdentityProviderException(
                    "No fue posible consultar el client de la API en Keycloak. HTTP " + response.statusCode());
        }
        final ClientRepresentation[] clients = readJson(response.body(), ClientRepresentation[].class);
        final int clientCount = clients == null ? 0 : clients.length;
        if (clientCount != 1) {
            throw new IdentityProviderException(
                    "El client '" + apiClientId + "' no se pudo resolver de forma inequivoca en Keycloak ("
                            + clientCount + " resultados).");
        }
        final ClientRepresentation client = clients[0];
        if (client == null
                || client.id() == null
                || client.id().isBlank()
                || !Objects.equals(client.clientId(), apiClientId)) {
            throw new IdentityProviderException(
                    "Keycloak devolvio un client interno invalido o distinto para '" + apiClientId + "'.");
        }
        return client.id();
    }

    /**
     * Obtiene la representación EXACTA del client role tal como la entrega Keycloak — se
     * reenvía sin reconstruirla, para no inventar un objeto con IDs falsos.
     */
    private KeycloakRoleRepresentation resolverClientRole(
            final String adminToken,
            final String clientUuid,
            final String roleName
    ) {
        final String url = adminUrl("/clients/" + clientUuid + "/roles/" + encode(roleName));
        final HttpRequest request = authorizedRequestBuilder(url, adminToken).GET().build();
        final HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            throw new IdentityProviderException(
                    "El client role '" + roleName + "' no existe en Keycloak para el client '"
                            + apiClientId + "'. HTTP " + response.statusCode());
        }
        final KeycloakRoleRepresentation role = readJson(response.body(), KeycloakRoleRepresentation.class);
        if (role == null || role.id() == null || role.id().isBlank() || !Objects.equals(role.name(), roleName)) {
            throw new IdentityProviderException(
                    "Keycloak devolvio un client role invalido o distinto para '" + roleName + "'.");
        }
        return role;
    }

    private boolean clientRoleAlreadyAssigned(
            final String adminToken,
            final String externalId,
            final String clientUuid,
            final String roleName
    ) {
        final String url = adminUrl("/users/" + externalId + "/role-mappings/clients/" + clientUuid);
        final HttpRequest request = authorizedRequestBuilder(url, adminToken).GET().build();
        final HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            throw new IdentityProviderException(
                    "No fue posible consultar client roles existentes en Keycloak. HTTP " + response.statusCode());
        }

        final KeycloakRoleRepresentation[] assignedRoles =
                readJson(response.body(), KeycloakRoleRepresentation[].class);
        return assignedRoles != null
                && Arrays.stream(assignedRoles)
                .filter(Objects::nonNull)
                .anyMatch(role -> Objects.equals(role.name(), roleName));
    }

    private void asignarClientRoleRepresentation(
            final String adminToken,
            final String externalId,
            final String clientUuid,
            final KeycloakRoleRepresentation roleRepresentation
    ) {
        final String url = adminUrl("/users/" + externalId + "/role-mappings/clients/" + clientUuid);
        final HttpRequest request = authorizedRequestBuilder(url, adminToken)
                .header("Content-Type", CONTENT_TYPE_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(writeJson(List.of(roleRepresentation))))
                .build();
        final HttpResponse<String> response = send(request);
        if (response.statusCode() != 204) {
            throw new IdentityProviderException("No se pudo asignar el client role en Keycloak. HTTP " + response.statusCode());
        }
    }

    /**
     * Traducción única y centralizada rol institucional → nombre exacto de client role en
     * Keycloak. Es literalmente {@code rolInstitucional.name()}: nunca códigos cortos, nunca
     * alias, nunca prefijo {@code ROLE_}. Application ya garantiza el tipo (enum), así que aquí
     * solo se valida null — no existe conversión de texto libre porque Application nunca envía
     * el rol como {@code String}. No se duplica esta traducción en ningún otro método.
     */
    private static String requireValidInstitutionalRoleName(final InstitutionalRole rolInstitucional) {
        if (rolInstitucional == null) {
            throw new IdentityProviderException("El rol institucional es obligatorio.");
        }
        return rolInstitucional.name();
    }

    // -------------------------------------------------------------------------
    // Eliminación (DELETE) — usada por eliminarCuenta()
    // -------------------------------------------------------------------------

    private void deleteUserBestEffort(final String adminToken, final String externalId) {
        try {
            deleteUser(adminToken, externalId);
        } catch (final Exception exception) {
            LOGGER.atError()
                    .addKeyValue("eventType", "IDENTITY_COMPENSATION_FAILED")
                    .addKeyValue("correlationId", CorrelationIdContext.getAsString())
                    .addKeyValue("externalUserId", externalId)
                    .addKeyValue("provider", PROVIDER_NAME)
                    .setCause(exception)
                    .log("No fue posible eliminar la cuenta de compensacion en Keycloak.");
        }
    }

    private void deleteUser(final String adminToken, final String externalId) {
        final String validatedExternalId = requireValidExternalUserId(
                externalId,
                "externalUserId es obligatorio para eliminar usuario en Keycloak."
        );
        final HttpRequest request = authorizedRequestBuilder(adminUrl("/users/" + validatedExternalId), adminToken)
                .DELETE()
                .build();
        final HttpResponse<String> response = send(request);
        if (response.statusCode() != 204 && response.statusCode() != 404) {
            throw new IdentityProviderException("No se pudo eliminar la cuenta en Keycloak. HTTP " + response.statusCode());
        }
    }

    private static String requireValidExternalUserId(final String externalId, final String message) {
        if (externalId == null || externalId.isBlank()) {
            throw new IdentityProviderException(message);
        }
        return externalId;
    }

    // -------------------------------------------------------------------------
    // Helpers HTTP/JSON centralizados (Authorization Bearer, envío, status, JSON)
    // -------------------------------------------------------------------------

    private String adminUrl(final String path) {
        return serverUrl + "/admin/realms/" + realm + path;
    }

    private HttpRequest.Builder authorizedRequestBuilder(final String url, final String adminToken) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", BEARER_PREFIX + adminToken)
                .timeout(TIMEOUT);
    }

    private HttpResponse<String> send(final HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IdentityProviderException("Error de comunicacion con Keycloak.", exception);
        } catch (final Exception exception) {
            throw new IdentityProviderException("Error de comunicacion con Keycloak.", exception);
        }
    }

    private static boolean isSuccess(final int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private String writeJson(final Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (final Exception exception) {
            throw new IdentityProviderException("Error al serializar payload para Keycloak.", exception);
        }
    }

    private <T> T readJson(final String body, final Class<T> type) {
        try {
            return objectMapper.readValue(body, type);
        } catch (final Exception exception) {
            throw new IdentityProviderException("Respuesta JSON invalida de Keycloak.", exception);
        }
    }

    private static String encode(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    // -------------------------------------------------------------------------
    // Modelos internos (Jackson) — nunca atraviesan el Port
    // -------------------------------------------------------------------------

    private record AdminTokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    private record UserCreationRequest(
            String username,
            String email,
            String firstName,
            String lastName,
            boolean enabled,
            boolean emailVerified,
            Map<String, List<String>> attributes
    ) {
    }

    private record UserSearchResult(
            String id,
            String username,
            String email,
            Map<String, List<String>> attributes
    ) {
    }

    private record ClientRepresentation(String id, String clientId) {
    }

    private record KeycloakRoleRepresentation(
            String id,
            String name,
            String description,
            Boolean composite,
            Boolean clientRole,
            String containerId
    ) {
    }

    private record CredentialRepresentation(String type, String value, boolean temporary) {
    }

    private record UserResolution(String externalId, boolean newlyCreated) {
    }
}
