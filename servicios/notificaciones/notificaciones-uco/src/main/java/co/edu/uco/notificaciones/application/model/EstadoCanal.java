package co.edu.uco.notificaciones.application.model;

import java.time.Instant;

/** Estado operativo del canal para la administracion. Nunca incluye la clave del SMTP. */
public record EstadoCanal(
        boolean canalActivo,
        boolean dispatcherEnabled,
        boolean disponible,
        String canal,
        String host,
        int port,
        String username,
        boolean tienePasswordConfigurado,
        String fromAddress,
        String fromName,
        boolean starttls,
        boolean ssl,
        int maxBcc,
        int ratePerMinute,
        int ratePerDay,
        String platformUrl,
        boolean testModeEnabled,
        int testModeRedirectCount,
        Instant ultimoEnvioUtc,
        String ultimoError
) {
}
