package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de runtime security (validación de JWT emitidos por Keycloak). Separada
 * deliberadamente de las properties administrativas de Identity ({@code KeycloakIdentityProviderProperties}):
 * aquí NO viven admin username/password/client-secret — eso pertenece al provisioning de
 * usuarios (Prompt 2B).
 */
@ConfigurationProperties(prefix = "app.providers.keycloak-security")
public record KeycloakSecurityProviderProperties(
        String issuerUri,
        String apiClientId,
        String expectedAudience,
        String userIdClaim
) {

    public KeycloakSecurityProviderProperties {
        requireNonBlank(issuerUri, "app.providers.keycloak-security.issuer-uri es obligatorio.");
        requireNonBlank(apiClientId, "app.providers.keycloak-security.api-client-id es obligatorio.");
        requireNonBlank(expectedAudience, "app.providers.keycloak-security.expected-audience es obligatorio.");
        requireNonBlank(userIdClaim, "app.providers.keycloak-security.user-id-claim es obligatorio.");
    }

    private static void requireNonBlank(final String value, final String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
    }
}
