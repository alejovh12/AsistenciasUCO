package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Propiedades del proveedor SMTP de notificaciones.
 */
@ConfigurationProperties(prefix = "app.providers.smtp-notification")
public record SmtpNotificationProperties(
        String host,
        @DefaultValue("587") Integer port,
        @DefaultValue("") String username,
        @DefaultValue("") String password,
        @DefaultValue("true") Boolean starttls,
        @DefaultValue("false") Boolean ssl,
        String fromAddress,
        @DefaultValue("Asistencia UCO") String fromName,
        @DefaultValue("10s") Duration connectTimeout,
        @DefaultValue("20s") Duration readTimeout,
        @DefaultValue("50") Integer maxBcc,
        @DefaultValue("60") Integer ratePerMinute,
        @DefaultValue("500") Integer ratePerDay,
        @DefaultValue("false") Boolean requireConfirmedEmail,
        @DefaultValue("") String platformUrl,
        @DefaultValue("60s") Duration templateCacheTtl,
        @DefaultValue TestMode testMode
) {

    private static final Pattern SAFE_EMAIL_PATTERN = Pattern.compile(
            "^[^@\\s,;<>\"()]+@[^@\\s,;<>\"().]+(?:\\.[^@\\s,;<>\"().]+)+$"
    );

    @ConstructorBinding
    public SmtpNotificationProperties {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("app.providers.smtp-notification.host es obligatorio.");
        }
        host = host.trim();
        port = port == null ? 587 : port;
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("app.providers.smtp-notification.port debe estar entre 1 y 65535.");
        }
        username = username == null ? "" : username;
        password = password == null ? "" : password;
        starttls = starttls == null ? Boolean.TRUE : starttls;
        ssl = ssl == null ? Boolean.FALSE : ssl;
        if (Boolean.TRUE.equals(starttls) && Boolean.TRUE.equals(ssl)) {
            throw new IllegalArgumentException("app.providers.smtp-notification.starttls y ssl no pueden estar ambos en true.");
        }
        if (!esCorreoSeguro(fromAddress)) {
            throw new IllegalArgumentException("app.providers.smtp-notification.from-address debe tener un formato de correo valido.");
        }
        fromAddress = fromAddress.trim();
        fromName = (fromName == null || fromName.isBlank()) ? "Asistencia UCO" : fromName.trim();
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(10) : connectTimeout;
        if (connectTimeout.isZero() || connectTimeout.isNegative()) {
            throw new IllegalArgumentException("app.providers.smtp-notification.connect-timeout debe ser mayor que cero.");
        }
        readTimeout = readTimeout == null ? Duration.ofSeconds(20) : readTimeout;
        if (readTimeout.isZero() || readTimeout.isNegative()) {
            throw new IllegalArgumentException("app.providers.smtp-notification.read-timeout debe ser mayor que cero.");
        }
        maxBcc = maxBcc == null ? 50 : maxBcc;
        if (maxBcc < 1 || maxBcc > 100) {
            throw new IllegalArgumentException("app.providers.smtp-notification.max-bcc debe estar entre 1 y 100.");
        }
        ratePerMinute = ratePerMinute == null ? 60 : ratePerMinute;
        if (ratePerMinute < 1 || ratePerMinute > 10000) {
            throw new IllegalArgumentException("app.providers.smtp-notification.rate-per-minute debe estar entre 1 y 10000.");
        }
        ratePerDay = ratePerDay == null ? 500 : ratePerDay;
        if (ratePerDay < ratePerMinute) {
            throw new IllegalArgumentException("app.providers.smtp-notification.rate-per-day debe ser >= rate-per-minute.");
        }
        requireConfirmedEmail = requireConfirmedEmail == null ? Boolean.FALSE : requireConfirmedEmail;
        platformUrl = platformUrl == null ? "" : platformUrl.trim();
        if (!platformUrl.isEmpty()
                && !platformUrl.startsWith("http://")
                && !platformUrl.startsWith("https://")) {
            throw new IllegalArgumentException("app.providers.smtp-notification.platform-url debe iniciar con http:// o https://.");
        }
        templateCacheTtl = templateCacheTtl == null ? Duration.ofSeconds(60) : templateCacheTtl;
        if (templateCacheTtl.isZero() || templateCacheTtl.isNegative()) {
            throw new IllegalArgumentException("app.providers.smtp-notification.template-cache-ttl debe ser mayor que cero.");
        }
        testMode = testMode == null ? new TestMode(false, List.of()) : testMode;
    }

    public SmtpNotificationProperties(
            final String host,
            final Integer port,
            final String username,
            final String password,
            final Boolean starttls,
            final Boolean ssl,
            final String fromAddress,
            final String fromName,
            final Duration connectTimeout,
            final Duration readTimeout,
            final Integer maxBcc,
            final Integer ratePerMinute,
            final Integer ratePerDay,
            final Boolean requireConfirmedEmail,
            final String platformUrl,
            final Duration templateCacheTtl
    ) {
        this(
                host,
                port,
                username,
                password,
                starttls,
                ssl,
                fromAddress,
                fromName,
                connectTimeout,
                readTimeout,
                maxBcc,
                ratePerMinute,
                ratePerDay,
                requireConfirmedEmail,
                platformUrl,
                templateCacheTtl,
                new TestMode(false, List.of())
        );
    }

    public SmtpNotificationProperties(
            final String host,
            final int port,
            final String username,
            final String password,
            final boolean starttls,
            final boolean ssl,
            final String fromAddress,
            final String fromName,
            final Duration connectTimeout,
            final Duration readTimeout,
            final int maxBcc,
            final int ratePerMinute,
            final int ratePerDay,
            final boolean requireConfirmedEmail,
            final String platformUrl
    ) {
        this(
                host,
                port,
                username,
                password,
                starttls,
                ssl,
                fromAddress,
                fromName,
                connectTimeout,
                readTimeout,
                maxBcc,
                ratePerMinute,
                ratePerDay,
                requireConfirmedEmail,
                platformUrl,
                Duration.ofSeconds(60),
                new TestMode(false, List.of())
        );
    }

    /**
     * Configuracion del modo de pruebas con redireccion de correo (D15, CONTRACT §3.1 y §4.6).
     */
    public record TestMode(
            Boolean enabled,
            List<String> redirectTo
    ) {
        private static final int MAX_REDIRECT_ADDRESSES = 5;

        public TestMode {
            enabled = Boolean.TRUE.equals(enabled);
            if (!enabled) {
                redirectTo = List.of();
            } else {
                if (redirectTo == null || redirectTo.isEmpty()) {
                    throw new IllegalArgumentException(
                            "app.providers.smtp-notification.test-mode.redirect-to es obligatorio cuando test-mode.enabled=true."
                    );
                }
                final Set<String> unicos = new LinkedHashSet<>();
                for (final String raw : redirectTo) {
                    if (!esCorreoSeguro(raw)) {
                        throw new IllegalArgumentException(
                                "app.providers.smtp-notification.test-mode.redirect-to contiene una direccion invalida o insegura."
                        );
                    }
                    unicos.add(raw.trim());
                }
                if (unicos.size() > MAX_REDIRECT_ADDRESSES) {
                    throw new IllegalArgumentException(
                            "app.providers.smtp-notification.test-mode.redirect-to admite entre 1 y 5 direcciones."
                    );
                }
                redirectTo = List.copyOf(unicos);
            }
        }

        @ConstructorBinding
        public TestMode(
                @DefaultValue("false") final Boolean enabled,
                @DefaultValue("") final String redirectTo
        ) {
            this(enabled, parsearCsv(enabled, redirectTo));
        }

        private static List<String> parsearCsv(final Boolean enabled, final String csv) {
            if (!Boolean.TRUE.equals(enabled)) {
                return List.of();
            }
            if (csv == null || csv.isBlank()) {
                return List.of();
            }
            return Arrays.asList(csv.split(",", -1));
        }
    }

    /**
     * Valida que una direccion de correo electronico tenga formato seguro de direccion unica,
     * rechazando nulos, blancos, saltos de linea, espacios, separadores de lista (, ;),
     * parentesis angulares (< >), comillas (") y parentesis (( )).
     */
    public static boolean esCorreoSeguro(final String correo) {
        if (correo == null || correo.isBlank()) {
            return false;
        }
        if (correo.indexOf('\r') >= 0 || correo.indexOf('\n') >= 0) {
            return false;
        }
        return SAFE_EMAIL_PATTERN.matcher(correo.trim()).matches();
    }

    @Override
    public String toString() {
        return "SmtpNotificationProperties["
                + "host=" + host
                + ", port=" + port
                + ", username=" + username
                + ", password=[PROTECTED]"
                + ", starttls=" + starttls
                + ", ssl=" + ssl
                + ", fromAddress=" + fromAddress
                + ", fromName=" + fromName
                + ", connectTimeout=" + connectTimeout
                + ", readTimeout=" + readTimeout
                + ", maxBcc=" + maxBcc
                + ", ratePerMinute=" + ratePerMinute
                + ", ratePerDay=" + ratePerDay
                + ", requireConfirmedEmail=" + requireConfirmedEmail
                + ", platformUrl=" + platformUrl
                + ", templateCacheTtl=" + templateCacheTtl
                + "]";
    }
}
