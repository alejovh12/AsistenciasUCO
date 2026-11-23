package co.edu.uco.notificaciones.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Configuracion del servicio. Los nombres de las variables de entorno del SMTP son los mismos que tenia el backend de
 * Asistencias ({@code SMTP_NOTIFICATION_*}) para que el traslado sea copiar los valores.
 */
@ConfigurationProperties(prefix = "notificaciones")
public record NotificacionesProperties(
        @DefaultValue("") String apiKey,
        @DefaultValue("log") String canal,
        @DefaultValue Smtp smtp,
        @DefaultValue Dispatcher dispatcher
) {

    /** Transporte SMTP, tope de envios y modo de pruebas (redirige todo a {@code redirectTo}). */
    public record Smtp(
            @DefaultValue("") String host,
            @DefaultValue("587") int port,
            @DefaultValue("") String username,
            @DefaultValue("") String password,
            @DefaultValue("true") boolean starttls,
            @DefaultValue("false") boolean ssl,
            @DefaultValue("") String fromAddress,
            @DefaultValue("Asistencia UCO") String fromName,
            @DefaultValue("10s") Duration connectTimeout,
            @DefaultValue("20s") Duration readTimeout,
            @DefaultValue("50") int maxBcc,
            @DefaultValue("60") int ratePerMinute,
            @DefaultValue("500") int ratePerDay,
            @DefaultValue("") String platformUrl,
            @DefaultValue TestMode testMode
    ) {
    }

    public record TestMode(@DefaultValue("false") boolean enabled, @DefaultValue("") String redirectTo) {
    }

    /** Despachador de la cola: lote, lease, reintentos con backoff y retencion de entregas terminales. */
    public record Dispatcher(
            @DefaultValue("true") boolean enabled,
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
    }
}
