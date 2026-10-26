package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.Locale;

/**
 * Cómo se conecta el aprovisionamiento administrativo (crear/habilitar/deshabilitar cuentas y asignar roles) al
 * realm de identidad central del Security Baseline. Es la contraparte de {@link SecurityBaselineProviderProperties}
 * (validación de JWT): ambos deben apuntar al mismo emisor. Nunca puede apuntar al Keycloak legacy.
 *
 * <p>{@code adminClientSecret} no tiene valor por defecto: sin él el arranque falla.</p>
 */
@ConfigurationProperties(prefix = "app.providers.security-baseline-identity")
public record SecurityBaselineIdentityProviderProperties(
        String serverUrl,
        String realm,
        String adminClientId,
        String adminClientSecret,
        String apiClientId,
        String userIdAttribute
) {

    private static final String PREFIX = "app.providers.security-baseline-identity.";

    public SecurityBaselineIdentityProviderProperties {
        requireNonBlank(serverUrl, PREFIX + "server-url es obligatorio.");
        requireNonBlank(realm, PREFIX + "realm es obligatorio.");
        requireNonBlank(adminClientId, PREFIX + "admin-client-id es obligatorio.");
        requireNonBlank(adminClientSecret, PREFIX + "admin-client-secret es obligatorio. No existe un valor por "
                + "defecto funcional por diseño: configure SECURITY_BASELINE_IDENTITY_ADMIN_CLIENT_SECRET.");
        requireNonBlank(apiClientId, PREFIX + "api-client-id es obligatorio.");
        requireNonBlank(userIdAttribute, PREFIX + "user-id-attribute es obligatorio.");
        final URI uri = URI.create(serverUrl.trim());
        if (uri.getPort() == 8081 || "asistencias-uco".equals(realm.trim().toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("El aprovisionamiento baseline no puede usar el Keycloak legacy "
                    + "(puerto 8081 / realm asistencias-uco).");
        }
    }

    /** Nunca incluye el secreto administrativo: este record puede terminar en logs o excepciones. */
    @Override
    public String toString() {
        return "SecurityBaselineIdentityProviderProperties[serverUrl=" + serverUrl + ", realm=" + realm
                + ", adminClientId=" + adminClientId + ", adminClientSecret=<redacted>, apiClientId=" + apiClientId
                + ", userIdAttribute=" + userIdAttribute + "]";
    }

    private static void requireNonBlank(final String value, final String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
    }
}
