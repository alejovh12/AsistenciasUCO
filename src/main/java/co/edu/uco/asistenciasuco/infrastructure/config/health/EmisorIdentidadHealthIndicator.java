package co.edu.uco.asistenciasuco.infrastructure.config.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

/**
 * OBS-10 (VAL-oleada8): salud del emisor de identidad (Keycloak de Security Baseline o el Keycloak de contingencia). Pide el
 * documento de descubrimiento OIDC ({@code <issuer>/.well-known/openid-configuration}) con un tiempo maximo corto.
 * <p>
 * Se publica en {@code /actuator/health} (componente {@code emisorIdentidad}) pero NO en el grupo {@code readiness}: los
 * tokens se validan con las claves (JWKS) ya cargadas, asi que una caida breve del emisor no impide atender peticiones con
 * tokens vigentes. Solo expone el emisor (dato publico) y el estado HTTP.
 */
public final class EmisorIdentidadHealthIndicator implements HealthIndicator {
    private static final String CLAVE_ISSUER = "issuer";

    static final Duration TIEMPO_MAXIMO = Duration.ofSeconds(3);
    private static final String DESCUBRIMIENTO = "/.well-known/openid-configuration";

    private final String issuer;
    private final URI descubrimiento;
    private final HttpClient httpClient;

    public EmisorIdentidadHealthIndicator(final String issuerUri) {
        this(issuerUri, HttpClient.newBuilder().connectTimeout(TIEMPO_MAXIMO).followRedirects(HttpClient.Redirect.NEVER).build());
    }

    EmisorIdentidadHealthIndicator(final String issuerUri, final HttpClient httpClient) {
        this.issuer = quitarBarraFinal(Objects.requireNonNull(issuerUri, "El emisor es obligatorio.").trim());
        this.descubrimiento = URI.create(issuer + DESCUBRIMIENTO);
        this.httpClient = Objects.requireNonNull(httpClient, "HttpClient es obligatorio.");
    }

    @Override
    public Health health() {
        try {
            final HttpResponse<Void> respuesta = httpClient.send(
                    HttpRequest.newBuilder(descubrimiento).timeout(TIEMPO_MAXIMO).GET().build(),
                    HttpResponse.BodyHandlers.discarding());
            final Health.Builder estado = respuesta.statusCode() == 200 ? Health.up() : Health.down();
            return estado.withDetail(CLAVE_ISSUER, issuer).withDetail("status", respuesta.statusCode()).build();
        } catch (final InterruptedException interrupcion) {
            Thread.currentThread().interrupt();
            return Health.down().withDetail(CLAVE_ISSUER, issuer).withDetail("error", interrupcion.getClass().getSimpleName()).build();
        } catch (final Exception fallo) {
            return Health.down().withDetail(CLAVE_ISSUER, issuer).withDetail("error", fallo.getClass().getSimpleName()).build();
        }
    }

    private static String quitarBarraFinal(final String valor) {
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }
}
