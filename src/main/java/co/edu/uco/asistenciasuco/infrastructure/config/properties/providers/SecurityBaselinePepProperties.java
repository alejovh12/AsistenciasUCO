package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/**
 * Configuración opt-in del PEP de Security Baseline. Cuando está desactivada no se exige ningún
 * dato del proveedor y el backend conserva exclusivamente su camino JWT actual.
 *
 * <p>Con el flag activo la URL del PEP debe ser un origen fijo: HTTPS, o HTTP únicamente hacia
 * loopback (laboratorio local). Cualquier despliegue compartido exige HTTPS.
 */
@ConfigurationProperties(prefix = "app.providers.security-baseline-pep")
public record SecurityBaselinePepProperties(
        boolean enabled,
        URI pepUrl,
        String applicationName,
        String environment,
        String applicationCredential,
        Duration connectTimeout,
        Duration requestTimeout
) {

    private static final String PREFIX = "app.providers.security-baseline-pep.";
    private static final String EMBEDDED_DECISION_PATH = "/internal/v1/embedded-access-decisions";
    private static final Duration MAX_TIMEOUT = Duration.ofSeconds(30);
    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]", "::1");
    private static final String IDENTIFIER = "[A-Za-z0-9_.:-]{1,128}";

    public SecurityBaselinePepProperties {
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
        requestTimeout = requestTimeout == null ? Duration.ofSeconds(5) : requestTimeout;
        if (enabled) {
            requireTrustedOrigin(pepUrl);
            requireIdentifier(applicationName, PREFIX + "application-name");
            requireIdentifier(environment, PREFIX + "environment");
            requireNonBlank(applicationCredential, PREFIX + "application-credential");
            requireBounded(connectTimeout, PREFIX + "connect-timeout");
            requireBounded(requestTimeout, PREFIX + "request-timeout");
        }
    }

    public URI embeddedDecisionEndpoint() {
        return pepUrl.resolve(EMBEDDED_DECISION_PATH);
    }

    /** Nunca incluye la credencial de aplicación: este record puede terminar en logs o excepciones. */
    @Override
    public String toString() {
        return "SecurityBaselinePepProperties[enabled=" + enabled
                + ", pepUrl=" + pepUrl
                + ", applicationName=" + applicationName
                + ", environment=" + environment
                + ", applicationCredential=" + (applicationCredential == null || applicationCredential.isBlank()
                        ? "<unset>" : "<redacted>")
                + ", connectTimeout=" + connectTimeout
                + ", requestTimeout=" + requestTimeout + "]";
    }

    private static void requireTrustedOrigin(final URI value) {
        final String property = PREFIX + "pep-url";
        if (value == null || !value.isAbsolute() || value.getHost() == null) {
            throw new IllegalStateException(property + " debe ser una URL HTTP(S) absoluta.");
        }
        if (value.getRawUserInfo() != null || value.getRawQuery() != null || value.getRawFragment() != null) {
            throw new IllegalStateException(property + " no debe incluir credenciales, query ni fragment.");
        }
        final String path = value.getRawPath();
        if (path != null && !path.isEmpty() && !"/".equals(path)) {
            throw new IllegalStateException(property + " debe ser solo un origen, sin path.");
        }
        final String scheme = value.getScheme().toLowerCase(Locale.ROOT);
        if ("https".equals(scheme)) {
            return;
        }
        if (!"http".equals(scheme)) {
            throw new IllegalStateException(property + " debe usar HTTPS.");
        }
        if (!LOOPBACK_HOSTS.contains(value.getHost().toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException(property
                    + " solo admite HTTP hacia loopback (laboratorio local); use HTTPS en ambientes compartidos.");
        }
    }

    private static void requireIdentifier(final String value, final String property) {
        if (value == null || !value.matches(IDENTIFIER)) {
            throw new IllegalStateException(property + " es obligatorio y debe cumplir " + IDENTIFIER + ".");
        }
    }

    private static void requireNonBlank(final String value, final String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(property + " es obligatorio cuando el flag está activo.");
        }
    }

    private static void requireBounded(final Duration value, final String property) {
        if (value.isZero() || value.isNegative() || value.compareTo(MAX_TIMEOUT) > 0) {
            throw new IllegalStateException(property + " debe ser positiva y no mayor que " + MAX_TIMEOUT + ".");
        }
    }
}
