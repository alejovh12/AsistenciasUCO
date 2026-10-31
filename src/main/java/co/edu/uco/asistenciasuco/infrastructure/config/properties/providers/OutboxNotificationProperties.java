package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.time.Duration;

/**
 * Propiedades del proveedor outbox de notificaciones.
 */
@ConfigurationProperties(prefix = "app.providers.outbox-notification")
public record OutboxNotificationProperties(
        @DefaultValue("LOG") Sender sender,
        @DefaultValue("true") boolean dispatcherEnabled,
        @DefaultValue("50") int batchSize,
        @DefaultValue("60s") Duration lease,
        @DefaultValue("30s") Duration sweeperInterval,
        @DefaultValue("5") int maxAttempts,
        @DefaultValue("30s") Duration backoffBase,
        @DefaultValue("15m") Duration backoffMax,
        @DefaultValue("30") int retentionDays,
        @DefaultValue("6h") Duration retentionInterval,
        @DefaultValue("") String instanceId
) {

    private static final SecureRandom RANDOM = new SecureRandom();

    @ConstructorBinding
    public OutboxNotificationProperties {
        sender = sender == null ? Sender.LOG : sender;
        validarBatchSize(batchSize);
        validarLease(lease);
        validarSweeperInterval(sweeperInterval);
        validarMaxAttempts(maxAttempts);
        validarBackoffs(backoffBase, backoffMax);
        validarRetentionDays(retentionDays);
        retentionInterval = retentionInterval == null ? Duration.ofHours(6) : retentionInterval;
        instanceId = (instanceId == null || instanceId.isBlank()) ? generateInstanceId() : instanceId;
    }

    private static void validarBatchSize(final int batchSize) {
        if (batchSize < 1 || batchSize > 500) {
            throw new IllegalArgumentException("app.providers.outbox-notification.batch-size debe estar entre 1 y 500.");
        }
    }

    private static void validarLease(final Duration lease) {
        if (lease == null || lease.compareTo(Duration.ofSeconds(5)) < 0 || lease.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("app.providers.outbox-notification.lease debe estar entre 5s y 1h.");
        }
    }

    private static void validarSweeperInterval(final Duration sweeperInterval) {
        if (sweeperInterval == null || sweeperInterval.compareTo(Duration.ofSeconds(1)) < 0 || sweeperInterval.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("app.providers.outbox-notification.sweeper-interval debe estar entre 1s y 1h.");
        }
    }

    private static void validarMaxAttempts(final int maxAttempts) {
        if (maxAttempts < 1 || maxAttempts > 20) {
            throw new IllegalArgumentException("app.providers.outbox-notification.max-attempts debe estar entre 1 y 20.");
        }
    }

    private static void validarBackoffs(final Duration backoffBase, final Duration backoffMax) {
        if (backoffBase == null || backoffBase.compareTo(Duration.ofSeconds(1)) < 0 || backoffBase.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("app.providers.outbox-notification.backoff-base debe estar entre 1s y 1h.");
        }
        if (backoffMax == null || backoffMax.compareTo(backoffBase) < 0 || backoffMax.compareTo(Duration.ofHours(24)) > 0) {
            throw new IllegalArgumentException("app.providers.outbox-notification.backoff-max debe ser >= backoff-base y <= 24h.");
        }
    }

    private static void validarRetentionDays(final int retentionDays) {
        if (retentionDays < 1 || retentionDays > 3650) {
            throw new IllegalArgumentException("app.providers.outbox-notification.retention-days debe estar entre 1 y 3650.");
        }
    }

    private static String generateInstanceId() {
        String host = "host";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException _) {
            // Se usa el valor por defecto "host" si no es posible resolver el nombre del host
        }
        final byte[] bytes = new byte[4];
        RANDOM.nextBytes(bytes);
        final StringBuilder hex = new StringBuilder();
        for (final byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return host + "-" + hex;
    }

    public enum Sender {
        LOG,
        SMTP
    }
}
