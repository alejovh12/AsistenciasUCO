package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.catalogoparametros;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort.ParameterCatalogException;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.CatalogoParametrosProviderProperties;
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
import java.util.Objects;
import java.util.Optional;

/**
 * INT-004: token {@code client_credentials} de la identidad de servicio del backend contra el Keycloak de Security Baseline.
 *
 * <p>El token se guarda en memoria hasta {@value #MARGEN_SEGUNDOS} s antes de vencer. Una sola peticion de token en vuelo
 * (metodo sincronizado). Ni el secreto ni el token se escriben en logs ni en los mensajes de error.</p>
 */
public final class CatalogoParametrosM2mTokenProvider implements CatalogoParametrosTokenProvider {

    static final long MARGEN_SEGUNDOS = 30;
    static final long VIGENCIA_POR_DEFECTO_SEGUNDOS = 60;

    private final CatalogoParametrosProviderProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private String accessToken;
    private Instant accessTokenExpiry = Instant.EPOCH;

    public CatalogoParametrosM2mTokenProvider(
            final CatalogoParametrosProviderProperties properties,
            final HttpClient httpClient,
            final ObjectMapper objectMapper,
            final Clock clock
    ) {
        this.properties = Objects.requireNonNull(properties, "Las propiedades del Catalogo de Parametros son obligatorias.");
        this.httpClient = Objects.requireNonNull(httpClient, "HttpClient es obligatorio.");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper es obligatorio.");
        this.clock = Objects.requireNonNull(clock, "Clock es obligatorio.");
    }

    @Override
    public synchronized Optional<String> tokenEnCache() {
        return vigente() ? Optional.of(accessToken) : Optional.empty();
    }

    @Override
    public synchronized void invalidar() {
        accessToken = null;
        accessTokenExpiry = Instant.EPOCH;
    }

    @Override
    public synchronized String token() {
        if (vigente()) {
            return accessToken;
        }
        final String form = "grant_type=client_credentials"
                + "&client_id=" + URLEncoder.encode(properties.clientId(), StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(properties.clientSecret(), StandardCharsets.UTF_8);
        final HttpResponse<String> response;
        try {
            response = httpClient.send(HttpRequest.newBuilder(properties.tokenUri())
                    .timeout(properties.timeout())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (final IOException exception) {
            throw new ParameterCatalogException("No fue posible pedir el token M2M del Catalogo de Parametros.", exception);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ParameterCatalogException("La peticion del token M2M del Catalogo de Parametros fue interrumpida.", exception);
        }
        if (response.statusCode() != 200) {
            throw new ParameterCatalogException("Keycloak no entrego token a la identidad de servicio del Catalogo de Parametros (HTTP "
                    + response.statusCode() + ").");
        }
        final JsonNode body;
        try {
            body = objectMapper.readTree(response.body());
        } catch (final RuntimeException exception) {
            throw new ParameterCatalogException("La respuesta de token del Catalogo de Parametros no es JSON valido.", exception);
        }
        final String token = body.path("access_token").asString(null);
        if (token == null || token.isBlank()) {
            throw new ParameterCatalogException("La respuesta de token de la identidad de servicio no trae access_token.");
        }
        final long vigencia = body.path("expires_in").asLong(VIGENCIA_POR_DEFECTO_SEGUNDOS);
        accessToken = token;
        accessTokenExpiry = clock.instant().plusSeconds(Math.max(0, vigencia - MARGEN_SEGUNDOS));
        return token;
    }

    private boolean vigente() {
        return accessToken != null && clock.instant().isBefore(accessTokenExpiry);
    }
}
