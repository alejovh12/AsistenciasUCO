package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Credencial de servicio y mapeo rol→perfil con que el backend asegura, de forma idempotente, el perfil de acceso de
 * las cuentas que registra en el PDP (API de administración del PDP, contrato 0.1.7).
 *
 * <p>Asignar un client role en el proveedor de identidad no concede acceso: el PDP decide por perfiles. La
 * sincronización está protegida por una bandera explícita y permanece deshabilitada por defecto. La credencial es la
 * del cliente de servicio {@code asistencias-provisioner} (client_credentials) y no se reutiliza desde la credencial
 * administrativa de identidad; sin la bandera y el secreto dedicado toda sincronización queda como PENDIENTE.</p>
 *
 * <p>{@code profiles} traduce cada rol institucional al nombre del perfil individual del PDP; la fuente única de esos
 * nombres es {@code infra/security-baseline/access-catalog.json} y una prueba verifica que coincidan.</p>
 */
@ConfigurationProperties(prefix = "app.providers.security-baseline-pdp-admin")
public record SecurityBaselinePdpAdminProperties(
        URI pdpUrl,
        URI tokenUrl,
        String clientId,
        String clientSecret,
        boolean enabled,
        String applicationName,
        Map<InstitutionalRole, String> profiles,
        Duration connectTimeout,
        Duration requestTimeout
) {

    private static final String PREFIX = "app.providers.security-baseline-pdp-admin.";
    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]", "::1");

    public SecurityBaselinePdpAdminProperties {
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
        requestTimeout = requestTimeout == null ? Duration.ofSeconds(8) : requestTimeout;
        requireTrustedOrigin(pdpUrl, PREFIX + "pdp-url");
        requireTrustedOrigin(tokenUrl, PREFIX + "token-url");
        requireNonBlank(clientId, PREFIX + "client-id");
        requireNonBlank(applicationName, PREFIX + "application-name");
        profiles = profiles == null ? Map.of() : Map.copyOf(profiles);
        for (final InstitutionalRole role : InstitutionalRole.values()) {
            requireNonBlank(profiles.get(role), PREFIX + "profiles." + role.name());
        }
    }

    /** Solo la bandera explícita y el secreto dedicado habilitan la sincronización. */
    public boolean configured() {
        return enabled && clientSecret != null && !clientSecret.isBlank();
    }

    public String profileFor(final InstitutionalRole role) {
        return profiles.get(role);
    }

    /** Nunca incluye el secreto: este record puede terminar en logs o excepciones. */
    @Override
    public String toString() {
        return "SecurityBaselinePdpAdminProperties[pdpUrl=" + pdpUrl
                + ", tokenUrl=" + tokenUrl
                + ", clientId=" + clientId
                + ", enabled=" + enabled
                + ", clientSecret=" + (configured() ? "<redacted>" : "<unset>")
                + ", applicationName=" + applicationName + "]";
    }

    private static void requireTrustedOrigin(final URI value, final String property) {
        if (value == null || !value.isAbsolute() || value.getHost() == null) {
            throw new IllegalStateException(property + " debe ser una URL HTTP(S) absoluta.");
        }
        if (value.getRawUserInfo() != null || value.getRawFragment() != null) {
            throw new IllegalStateException(property + " no debe incluir credenciales ni fragment.");
        }
        final String scheme = value.getScheme();
        final boolean https = "https".equalsIgnoreCase(scheme);
        final boolean loopbackHttp = "http".equalsIgnoreCase(scheme) && LOOPBACK_HOSTS.contains(value.getHost().toLowerCase());
        if (!https && !loopbackHttp) {
            throw new IllegalStateException(property + " exige HTTPS (HTTP solo hacia loopback).");
        }
    }

    private static void requireNonBlank(final String value, final String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(property + " es obligatorio.");
        }
    }
}
