package co.edu.uco.notificaciones.infrastructure.config;

import co.edu.uco.notificaciones.application.model.ConfiguracionCanal;
import co.edu.uco.notificaciones.application.model.ConfiguracionDespachador;
import co.edu.uco.notificaciones.domain.Correos;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Valida las propiedades (falla al arrancar con un mensaje claro) y las traduce a los modelos de la aplicacion. */
final class ConfiguracionFactory {

    private static final int MAX_REDIRECCIONES = 5;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private ConfiguracionFactory() {
    }

    static ConfiguracionCanal canal(final NotificacionesProperties p) {
        final String canal = p.canal() == null ? "" : p.canal().trim().toLowerCase(java.util.Locale.ROOT);
        if (!ConfiguracionCanal.CANAL_SMTP.equals(canal) && !ConfiguracionCanal.CANAL_LOG.equals(canal)) {
            throw new IllegalArgumentException("notificaciones.canal debe ser 'smtp' o 'log'.");
        }
        final NotificacionesProperties.Smtp s = p.smtp();
        if (s.starttls() && s.ssl()) {
            throw new IllegalArgumentException("SMTP_NOTIFICATION_STARTTLS y SMTP_NOTIFICATION_SSL no pueden estar ambos en true.");
        }
        if (s.port() < 1 || s.port() > 65535) {
            throw new IllegalArgumentException("SMTP_NOTIFICATION_PORT debe estar entre 1 y 65535.");
        }
        if (s.maxBcc() < 1 || s.maxBcc() > 100) {
            throw new IllegalArgumentException("SMTP_NOTIFICATION_MAX_BCC debe estar entre 1 y 100.");
        }
        if (s.ratePerMinute() < 1 || s.ratePerMinute() > 10000) {
            throw new IllegalArgumentException("SMTP_NOTIFICATION_RATE_PER_MINUTE debe estar entre 1 y 10000.");
        }
        if (s.ratePerDay() < s.ratePerMinute()) {
            throw new IllegalArgumentException("SMTP_NOTIFICATION_RATE_PER_DAY debe ser >= RATE_PER_MINUTE.");
        }
        final String platformUrl = s.platformUrl() == null ? "" : s.platformUrl().trim();
        if (!platformUrl.isEmpty() && !platformUrl.startsWith("http://") && !platformUrl.startsWith("https://")) {
            throw new IllegalArgumentException("SMTP_NOTIFICATION_PLATFORM_URL debe iniciar con http:// o https://.");
        }
        final boolean smtp = ConfiguracionCanal.CANAL_SMTP.equals(canal);
        if (smtp && (s.host() == null || s.host().isBlank())) {
            throw new IllegalArgumentException("SMTP_NOTIFICATION_HOST es obligatorio con NOTIFICACIONES_CANAL=smtp.");
        }
        final boolean hayRemitente = s.fromAddress() != null && !s.fromAddress().isBlank();
        if ((smtp || hayRemitente) && !Correos.esCorreoSeguro(s.fromAddress())) {
            throw new IllegalArgumentException("SMTP_NOTIFICATION_FROM_ADDRESS debe tener un formato de correo valido.");
        }
        final List<String> redirecciones = redirecciones(s.testMode());
        return new ConfiguracionCanal(canal, s.host() == null ? "" : s.host().trim(), s.port(), nulo(s.username()),
                s.password() != null && !s.password().isBlank(), hayRemitente ? s.fromAddress().trim() : "notificaciones@localhost.localdomain",
                s.fromName() == null || s.fromName().isBlank() ? "Asistencia UCO" : s.fromName().trim(), s.starttls(), s.ssl(),
                s.maxBcc(), s.ratePerMinute(), s.ratePerDay(), platformUrl, s.testMode().enabled(), redirecciones);
    }

    private static List<String> redirecciones(final NotificacionesProperties.TestMode modo) {
        if (!modo.enabled()) {
            return List.of();
        }
        final Set<String> unicas = new LinkedHashSet<>();
        for (final String candidata : Arrays.asList(modo.redirectTo() == null ? new String[0] : modo.redirectTo().split(",", -1))) {
            if (!Correos.esCorreoSeguro(candidata)) {
                throw new IllegalArgumentException("SMTP_NOTIFICATION_TEST_MODE_REDIRECT_TO contiene una direccion invalida o vacia (1 a "
                        + MAX_REDIRECCIONES + " direcciones separadas por coma; es obligatorio con el modo de pruebas).");
            }
            unicas.add(candidata.trim());
        }
        if (unicas.isEmpty() || unicas.size() > MAX_REDIRECCIONES) {
            throw new IllegalArgumentException("SMTP_NOTIFICATION_TEST_MODE_REDIRECT_TO admite entre 1 y " + MAX_REDIRECCIONES + " direcciones.");
        }
        return List.copyOf(unicas);
    }

    static ConfiguracionDespachador despachador(final NotificacionesProperties.Dispatcher d) {
        if (d.batchSize() < 1 || d.batchSize() > 500) {
            throw new IllegalArgumentException("DISPATCHER_BATCH_SIZE debe estar entre 1 y 500.");
        }
        if (d.lease() == null || d.lease().compareTo(Duration.ofSeconds(5)) < 0 || d.lease().compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("DISPATCHER_LEASE debe estar entre 5s y 1h.");
        }
        if (d.sweeperInterval() == null || d.sweeperInterval().compareTo(Duration.ofSeconds(1)) < 0 || d.sweeperInterval().compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("DISPATCHER_SWEEPER_INTERVAL debe estar entre 1s y 1h.");
        }
        if (d.maxAttempts() < 1 || d.maxAttempts() > 20) {
            throw new IllegalArgumentException("DISPATCHER_MAX_ATTEMPTS debe estar entre 1 y 20.");
        }
        if (d.backoffBase() == null || d.backoffBase().compareTo(Duration.ofSeconds(1)) < 0 || d.backoffBase().compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("DISPATCHER_BACKOFF_BASE debe estar entre 1s y 1h.");
        }
        if (d.backoffMax() == null || d.backoffMax().compareTo(d.backoffBase()) < 0 || d.backoffMax().compareTo(Duration.ofHours(24)) > 0) {
            throw new IllegalArgumentException("DISPATCHER_BACKOFF_MAX debe ser >= DISPATCHER_BACKOFF_BASE y <= 24h.");
        }
        if (d.retentionDays() < 1 || d.retentionDays() > 3650) {
            throw new IllegalArgumentException("DISPATCHER_RETENTION_DAYS debe estar entre 1 y 3650.");
        }
        final Duration intervaloRetencion = d.retentionInterval() == null ? Duration.ofHours(6) : d.retentionInterval();
        final String instancia = d.instanceId() == null || d.instanceId().isBlank() ? generarInstancia() : d.instanceId().trim();
        return new ConfiguracionDespachador(d.enabled(), d.batchSize(), d.lease(), d.sweeperInterval(), d.maxAttempts(), d.backoffBase(),
                d.backoffMax(), d.retentionDays(), intervaloRetencion, instancia);
    }

    static String generarInstancia() {
        String host = "host";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException ex) {
            // Se conserva "host" si el nombre no se resuelve.
        }
        final byte[] bytes = new byte[4];
        ALEATORIO.nextBytes(bytes);
        final StringBuilder hex = new StringBuilder();
        for (final byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return host + "-" + hex;
    }

    private static String nulo(final String valor) {
        return valor == null ? "" : valor;
    }
}
