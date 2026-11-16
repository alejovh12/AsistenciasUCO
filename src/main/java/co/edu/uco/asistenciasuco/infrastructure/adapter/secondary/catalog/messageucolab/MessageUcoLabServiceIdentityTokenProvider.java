package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.messageucolab;

import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.MessageUcoLabServiceIdentityProperties;
import co.edu.uco.messageucolab.client.http.MessageUcoLabAccessTokenProvider;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
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

/**
 * INT-003: token de la identidad de servicio del backend para MessageUcoLab 0.3.0 (grant {@code client_credentials}
 * contra el Keycloak del componente de Seguridad).
 *
 * <p>El starter 0.1.3 busca por defecto el {@code ServiceAccessTokenProvider} de
 * {@code security-pep-integration-spring-boot-starter}, que exige Spring Boot 4.1.1 y trae su propia seguridad web. El
 * backend ya obtiene tokens de servicio con su propio cliente (como {@code SecurityBaselinePdpProfileClient}), asi que
 * registra este bean: el starter lo usa porque su proveedor es {@code @ConditionalOnMissingBean}.</p>
 *
 * <p>El token se guarda en memoria hasta 30 s antes de vencer. Un 401 de MessageUcoLab no se reintenta aqui: el
 * siguiente pedido vuelve a usar el token hasta su vencimiento; si Keycloak lo revoca, {@link #invalidar()} lo descarta.</p>
 */
public final class MessageUcoLabServiceIdentityTokenProvider implements MessageUcoLabAccessTokenProvider {

    static final long MARGEN_SEGUNDOS = 30;
    static final long VIGENCIA_POR_DEFECTO_SEGUNDOS = 60;

    private final MessageUcoLabServiceIdentityProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private String accessToken;
    private Instant accessTokenExpiry = Instant.EPOCH;

    public MessageUcoLabServiceIdentityTokenProvider(
            final MessageUcoLabServiceIdentityProperties properties,
            final HttpClient httpClient,
            final ObjectMapper objectMapper,
            final Clock clock
    ) {
        this.properties = Objects.requireNonNull(properties, "La identidad de servicio de MessageUcoLab es obligatoria.");
        this.httpClient = Objects.requireNonNull(httpClient, "HttpClient es obligatorio.");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper es obligatorio.");
        this.clock = Objects.requireNonNull(clock, "Clock es obligatorio.");
    }

    @Override
    public Mono<String> token() {
        return Mono.fromCallable(this::tokenVigente).subscribeOn(Schedulers.boundedElastic());
    }

    /** Descarta el token en memoria (el siguiente pedido pide uno nuevo). */
    public synchronized void invalidar() {
        accessToken = null;
        accessTokenExpiry = Instant.EPOCH;
    }

    synchronized String tokenVigente() throws IOException, InterruptedException {
        if (accessToken != null && clock.instant().isBefore(accessTokenExpiry)) {
            return accessToken;
        }
        final String form = "grant_type=client_credentials"
                + "&client_id=" + URLEncoder.encode(properties.clientId(), StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(properties.clientSecret(), StandardCharsets.UTF_8);
        final HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder(properties.tokenUri())
                .timeout(properties.requestTimeout())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Keycloak no entrego token a la identidad de servicio de MessageUcoLab (HTTP "
                    + response.statusCode() + ").");
        }
        final JsonNode body = objectMapper.readTree(response.body());
        final String token = body.path("access_token").asString(null);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("La respuesta de token de la identidad de servicio no trae access_token.");
        }
        final long vigencia = body.path("expires_in").asLong(VIGENCIA_POR_DEFECTO_SEGUNDOS);
        accessToken = token;
        accessTokenExpiry = clock.instant().plusSeconds(Math.max(0, vigencia - MARGEN_SEGUNDOS));
        return token;
    }
}
