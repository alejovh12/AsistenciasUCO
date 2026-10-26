package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline;

import co.edu.uco.asistenciasuco.application.secondaryports.security.AccessProfilePort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SecurityBaselinePdpAdminProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Asegura de forma idempotente los perfiles de acceso de una persona en el PDP de Security Baseline usando los
 * recursos REST de administración que el propio proyecto ya consume (aplicaciones, perfiles, roles, usuarios y
 * asignaciones de perfil). No inventa endpoints ni toca la base interna del PDP.
 *
 * <p>Autenticación: Bearer del cliente de servicio (client_credentials). Si el PDP no acepta la credencial, no
 * responde o responde 5xx, el resultado es {@code PENDIENTE_NO_DISPONIBLE}: la persona no recibe acceso y la
 * reconciliación puede repetirse sin efectos duplicados (solo asigna si falta y solo revoca asignaciones vigentes).</p>
 *
 * <p>Un perfil asignado a quien ya tiene directamente un rol del perfil lo rechaza el PDP (409): si la persona ya
 * tiene el rol directo vigente, el acceso efectivo existe y se informa como asignado; no se toca esa asignación.</p>
 */
public final class SecurityBaselinePdpProfileClient implements AccessProfilePort {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityBaselinePdpProfileClient.class);
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 200;

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final SecurityBaselinePdpAdminProperties properties;
    private final Clock clock;

    private volatile String accessToken;
    private volatile Instant accessTokenExpiry = Instant.EPOCH;
    private volatile String applicationId;

    public SecurityBaselinePdpProfileClient(
            final HttpClient httpClient,
            final ObjectMapper mapper,
            final SecurityBaselinePdpAdminProperties properties,
            final Clock clock
    ) {
        this.httpClient = Objects.requireNonNull(httpClient, "HttpClient es obligatorio.");
        this.mapper = Objects.requireNonNull(mapper, "ObjectMapper es obligatorio.");
        this.properties = Objects.requireNonNull(properties, "Las propiedades del PDP son obligatorias.");
        this.clock = Objects.requireNonNull(clock, "Clock es obligatorio.");
    }

    @Override
    public Resultado sincronizar(final String correo, final InstitutionalRole rol) {
        return operar(correo, rol, Modo.ASEGURAR);
    }

    @Override
    public Resultado revocar(final String correo, final InstitutionalRole rol) {
        return operar(correo, rol, Modo.REVOCAR);
    }

    @Override
    public Resultado consultar(final String correo, final InstitutionalRole rol) {
        return operar(correo, rol, Modo.CONSULTAR);
    }

    private enum Modo { ASEGURAR, REVOCAR, CONSULTAR }

    private Resultado operar(final String correo, final InstitutionalRole rol, final Modo modo) {
        if (correo == null || correo.isBlank() || rol == null) {
            return new Resultado(Estado.RECHAZADO, "Correo y rol son obligatorios para sincronizar el perfil.");
        }
        if (!properties.configured()) {
            return new Resultado(Estado.PENDIENTE_NO_DISPONIBLE,
                    "La credencial de servicio del PDP no esta configurada; el acceso no se concede hasta sincronizar");
        }
        try {
            final String appId = applicationId();
            final String profileName = properties.profileFor(rol);
            final Optional<String> profileId = idPorNombre("/api/v1/applications/" + appId + "/security/profiles", profileName);
            if (profileId.isEmpty()) {
                return new Resultado(Estado.RECHAZADO,
                        "El perfil '" + profileName + "' no existe en el PDP (ejecute provision_asistencias.py pdp)");
            }
            final Optional<String> userId = usuarioPorCorreo(correo);
            if (userId.isEmpty()) {
                return modo == Modo.REVOCAR
                        ? new Resultado(Estado.NO_ASIGNADO, "La persona no existe en el PDP: no hay nada que revocar")
                        : new Resultado(Estado.PENDIENTE_PERSONA_NO_REGISTRADA,
                                "La persona aun no existe en el PDP (se registra en su primer acceso)");
            }
            final List<String> vigentes = asignacionesVigentes(profileId.get(), userId.get());
            return switch (modo) {
                case CONSULTAR -> estadoActual(appId, rol, userId.get(), vigentes);
                case ASEGURAR -> asegurar(appId, profileId.get(), userId.get(), rol, vigentes);
                case REVOCAR -> revocarVigentes(appId, profileId.get(), vigentes, rol, userId.get());
            };
        } catch (final PdpException exception) {
            LOGGER.atWarn()
                    .addKeyValue("operation", "PDP_PROFILE_SYNC")
                    .addKeyValue("role", rol)
                    .addKeyValue("status", exception.status)
                    .log("El PDP no pudo completar la operacion de perfil: {}", exception.getMessage());
            return new Resultado(exception.estado, exception.getMessage());
        } catch (final IOException exception) {
            return new Resultado(Estado.PENDIENTE_NO_DISPONIBLE, "El PDP no respondio: " + exception.getClass().getSimpleName());
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new Resultado(Estado.PENDIENTE_NO_DISPONIBLE, "Operacion interrumpida");
        } catch (final RuntimeException exception) {
            return new Resultado(Estado.PENDIENTE_NO_DISPONIBLE,
                    "Respuesta inesperada del PDP: " + exception.getClass().getSimpleName());
        }
    }

    private Resultado estadoActual(
            final String appId, final InstitutionalRole rol, final String userId, final List<String> vigentes
    ) throws IOException, InterruptedException {
        if (!vigentes.isEmpty()) {
            return new Resultado(Estado.ASIGNADO, "Perfil asignado");
        }
        return tieneRolDirecto(appId, rol, userId)
                ? new Resultado(Estado.ASIGNADO, "Acceso por rol directo vigente")
                : new Resultado(Estado.NO_ASIGNADO, "Perfil no asignado");
    }

    private Resultado asegurar(
            final String appId, final String profileId, final String userId, final InstitutionalRole rol,
            final List<String> vigentes
    ) throws IOException, InterruptedException {
        if (!vigentes.isEmpty()) {
            return new Resultado(Estado.ASIGNADO, "Perfil ya asignado");
        }
        final HttpResponse<String> response = send("POST", "/api/v1/profiles/" + profileId + "/assignments",
                Map.of("profileId", profileId, "userId", userId, "applicationId", appId));
        if (response.statusCode() == 201 || response.statusCode() == 200) {
            return new Resultado(Estado.ASIGNADO, "Perfil asignado");
        }
        if (response.statusCode() == 409) {
            if (tieneRolDirecto(appId, rol, userId)) {
                return new Resultado(Estado.ASIGNADO, "Acceso por rol directo vigente (el PDP rechaza duplicar con perfil)");
            }
            return new Resultado(Estado.RECHAZADO, "El PDP rechazo la asignacion del perfil (conflicto 409)");
        }
        throw fallo(response.statusCode());
    }

    private Resultado revocarVigentes(
            final String appId, final String profileId, final List<String> vigentes, final InstitutionalRole rol,
            final String userId
    ) throws IOException, InterruptedException {
        for (final String assignmentId : vigentes) {
            final HttpResponse<String> response = send("DELETE",
                    "/api/v1/profiles/" + profileId + "/assignments/" + assignmentId, null);
            if (response.statusCode() != 200 && response.statusCode() != 204 && response.statusCode() != 404) {
                throw fallo(response.statusCode());
            }
        }
        if (tieneRolDirecto(appId, rol, userId)) {
            return new Resultado(Estado.RECHAZADO,
                    "La persona conserva el rol " + rol + " asignado directamente en el PDP (requiere revision manual)");
        }
        return new Resultado(Estado.NO_ASIGNADO, vigentes.isEmpty() ? "Perfil no asignado" : "Perfil revocado");
    }

    private boolean tieneRolDirecto(final String appId, final InstitutionalRole rol, final String userId)
            throws IOException, InterruptedException {
        final Optional<String> roleId = idPorNombre("/api/v1/applications/" + appId + "/security/roles", rol.name());
        if (roleId.isEmpty()) {
            return false;
        }
        final Instant now = clock.instant();
        for (final JsonNode assignment : paginado("/api/v1/roles/" + roleId.get() + "/assignments")) {
            if (userId.equals(texto(assignment, "userId")) && vigente(assignment, now)) {
                return true;
            }
        }
        return false;
    }

    private List<String> asignacionesVigentes(final String profileId, final String userId)
            throws IOException, InterruptedException {
        final Instant now = clock.instant();
        final List<String> ids = new ArrayList<>();
        for (final JsonNode assignment : paginado("/api/v1/profiles/" + profileId + "/assignments")) {
            if (userId.equals(texto(assignment, "userId")) && vigente(assignment, now)) {
                ids.add(texto(assignment, "id"));
            }
        }
        return ids;
    }

    private Optional<String> usuarioPorCorreo(final String correo) throws IOException, InterruptedException {
        final String esperado = correo.trim().toLowerCase(Locale.ROOT);
        for (final JsonNode user : paginado("/api/v1/users")) {
            final String email = texto(user, "email");
            if (email != null && email.trim().toLowerCase(Locale.ROOT).equals(esperado)) {
                return Optional.ofNullable(texto(user, "id"));
            }
        }
        return Optional.empty();
    }

    private String applicationId() throws IOException, InterruptedException {
        final String cached = applicationId;
        if (cached != null) {
            return cached;
        }
        for (final JsonNode application : paginado("/api/v1/applications")) {
            if (properties.applicationName().equals(texto(application, "name"))) {
                applicationId = texto(application, "id");
                return applicationId;
            }
        }
        throw new PdpException(Estado.RECHAZADO,
                "La aplicacion '" + properties.applicationName() + "' no existe en el PDP", 404);
    }

    private Optional<String> idPorNombre(final String path, final String nombre) throws IOException, InterruptedException {
        for (final JsonNode item : paginado(path)) {
            if (nombre.equals(texto(item, "name"))) {
                return Optional.ofNullable(texto(item, "id"));
            }
        }
        return Optional.empty();
    }

    /** Todas las filas de un listado del PDP (tamaño de página máximo 100; algunos listados no se paginan). */
    private List<JsonNode> paginado(final String path) throws IOException, InterruptedException {
        final List<JsonNode> filas = new ArrayList<>();
        for (int page = 0; page < MAX_PAGES; page++) {
            final HttpResponse<String> response = send("GET", path + "?page=" + page + "&size=" + PAGE_SIZE, null);
            if (response.statusCode() != 200) {
                throw fallo(response.statusCode());
            }
            final JsonNode data = mapper.readTree(response.body()).path("data");
            if (data.isArray()) {
                data.forEach(filas::add);
                return filas;
            }
            final JsonNode content = data.path("content");
            if (!content.isArray() || content.isEmpty()) {
                return filas;
            }
            content.forEach(filas::add);
            if (filas.size() >= data.path("total").asLong(0)) {
                return filas;
            }
        }
        return filas;
    }

    private HttpResponse<String> send(final String method, final String path, final Object body)
            throws IOException, InterruptedException {
        final HttpRequest.Builder builder = HttpRequest.newBuilder(properties.pdpUrl().resolve(path))
                .timeout(properties.requestTimeout())
                .header("Authorization", "Bearer " + token())
                .header("Accept", "application/json");
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
        }
        final HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            invalidarToken();
        }
        return response;
    }

    private synchronized String token() throws IOException, InterruptedException {
        if (accessToken != null && clock.instant().isBefore(accessTokenExpiry)) {
            return accessToken;
        }
        final String form = "grant_type=client_credentials"
                + "&client_id=" + URLEncoder.encode(properties.clientId(), StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(properties.clientSecret(), StandardCharsets.UTF_8);
        final HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder(properties.tokenUrl())
                .timeout(properties.requestTimeout())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new PdpException(Estado.PENDIENTE_NO_DISPONIBLE,
                    "El proveedor de identidad no entrego token de servicio para el PDP (HTTP " + response.statusCode() + ")",
                    response.statusCode());
        }
        final JsonNode body = mapper.readTree(response.body());
        final String token = texto(body, "access_token");
        if (token == null) {
            throw new PdpException(Estado.PENDIENTE_NO_DISPONIBLE, "Respuesta de token sin access_token", 200);
        }
        accessToken = token;
        accessTokenExpiry = clock.instant().plusSeconds(Math.max(0, body.path("expires_in").asLong(60) - 30));
        return token;
    }

    private void invalidarToken() {
        accessToken = null;
        accessTokenExpiry = Instant.EPOCH;
    }

    private static PdpException fallo(final int status) {
        if (status == 401 || status == 403) {
            return new PdpException(Estado.PENDIENTE_NO_DISPONIBLE,
                    "El PDP no acepto la credencial de servicio de la aplicacion (HTTP " + status + ")", status);
        }
        if (status >= 500 || status == 429 || status == 408) {
            return new PdpException(Estado.PENDIENTE_NO_DISPONIBLE, "El PDP no esta disponible (HTTP " + status + ")", status);
        }
        return new PdpException(Estado.RECHAZADO, "El PDP rechazo la operacion (HTTP " + status + ")", status);
    }

    private static boolean vigente(final JsonNode assignment, final Instant now) {
        final String until = texto(assignment, "validUntil");
        if (until == null || until.isBlank()) {
            return true;
        }
        try {
            return Instant.parse(until).isAfter(now);
        } catch (final java.time.format.DateTimeParseException exception) {
            return false;
        }
    }

    private static String texto(final JsonNode node, final String field) {
        final JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asString();
    }

    private static final class PdpException extends RuntimeException {
        private final transient Estado estado;
        private final int status;

        PdpException(final Estado estado, final String message, final int status) {
            super(message);
            this.estado = estado;
            this.status = status;
        }
    }
}
