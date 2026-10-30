package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
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
        @DefaultValue("60s") Duration templateCacheTtl
) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$"
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
        if (fromAddress == null || fromAddress.isBlank() || containsLineBreak(fromAddress)
                || !EMAIL_PATTERN.matcher(fromAddress.trim()).matches()) {
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
                Duration.ofSeconds(60)
        );
    }

    private static boolean containsLineBreak(final String value) {
        return value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0;
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
