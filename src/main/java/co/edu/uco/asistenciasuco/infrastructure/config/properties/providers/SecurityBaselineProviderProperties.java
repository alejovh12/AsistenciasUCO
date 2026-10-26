package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración verificable del emisor OIDC central del modo baseline.
 *
 * <p>{@code jwkSetUri} es opcional y solo cambia el TRANSPORTE de las claves públicas (p. ej. {@code 127.0.0.1} cuando el
 * resolvedor del sistema no resuelve {@code *.localhost}); el {@code iss} del token se sigue validando contra
 * {@code issuerUri}. Vacío: descubrimiento OIDC estándar.</p>
 */
@ConfigurationProperties(prefix = "app.providers.security-baseline")
public record SecurityBaselineProviderProperties(
        String issuerUri,
        String apiClientId,
        String expectedAudience,
        String emailClaim,
        String emailVerifiedClaim,
        String userIdClaim,
        String jwkSetUri
) {

    public SecurityBaselineProviderProperties {
        requireNonBlank(issuerUri, "app.providers.security-baseline.issuer-uri es obligatorio.");
        requireNonBlank(apiClientId, "app.providers.security-baseline.api-client-id es obligatorio.");
        requireNonBlank(expectedAudience, "app.providers.security-baseline.expected-audience es obligatorio.");
        requireNonBlank(emailClaim, "app.providers.security-baseline.email-claim es obligatorio.");
        requireNonBlank(emailVerifiedClaim, "app.providers.security-baseline.email-verified-claim es obligatorio.");
        requireNonBlank(userIdClaim, "app.providers.security-baseline.user-id-claim es obligatorio.");
        if (issuerUri.contains(":8081/") || issuerUri.contains("asistencias-uco")) {
            throw new IllegalStateException("El modo baseline no puede usar el issuer legacy de Keycloak.");
        }
    }

    private static void requireNonBlank(final String value, final String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
    }
}
