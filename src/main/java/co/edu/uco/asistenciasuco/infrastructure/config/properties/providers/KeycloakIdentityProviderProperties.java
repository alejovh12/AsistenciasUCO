package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de CÓMO el adapter de provisioning administrativo se conecta a Keycloak
 * (no QUÉ adapter se usa; eso lo decide {@code app.adapters.identity.provider}).
 *
 * <p>Separada por completo de {@code KeycloakSecurityProviderProperties} (runtime security):
 * esta clase solo existe para operaciones administrativas contra la Admin REST API de
 * Keycloak (crear/verificar/eliminar usuarios, asignar client roles). NO participa en la
 * validación de JWT de cada request.</p>
 *
 * <p>{@code adminClientSecret} deliberadamente NO tiene un default funcional (como
 * {@code change-me}, {@code admin}, {@code secret} o {@code password}): si el provider de
 * identity es Keycloak y falta el secret, el arranque debe fallar en vez de arrancar con una
 * credencial administrativa débil o adivinada.</p>
 */
@ConfigurationProperties(prefix = "app.providers.keycloak-identity")
public record KeycloakIdentityProviderProperties(
        String serverUrl,
        String realm,
        String adminClientId,
        String adminClientSecret,
        String apiClientId,
        String userIdAttribute
) {

    public KeycloakIdentityProviderProperties {
        requireNonBlank(serverUrl, "app.providers.keycloak-identity.server-url es obligatorio.");
        requireNonBlank(realm, "app.providers.keycloak-identity.realm es obligatorio.");
        requireNonBlank(adminClientId, "app.providers.keycloak-identity.admin-client-id es obligatorio.");
        requireNonBlank(
                adminClientSecret,
                "app.providers.keycloak-identity.admin-client-secret es obligatorio. "
                        + "No existe un valor por defecto funcional por diseño: configure "
                        + "KEYCLOAK_ADMIN_CLIENT_SECRET con el secret real del client "
                        + "confidencial de service account."
        );
        requireNonBlank(apiClientId, "app.providers.keycloak-identity.api-client-id es obligatorio.");
        requireNonBlank(userIdAttribute, "app.providers.keycloak-identity.user-id-attribute es obligatorio.");
    }

    private static void requireNonBlank(final String value, final String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
    }
}
