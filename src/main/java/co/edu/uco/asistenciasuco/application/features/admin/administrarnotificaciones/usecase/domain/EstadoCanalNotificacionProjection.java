package co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain;

import java.time.Instant;

public record EstadoCanalNotificacionProjection(
        String provider,
        String sender,
        boolean dispatcherEnabled,
        boolean disponible,
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
        boolean requireConfirmedEmail,
        String platformUrl,
        boolean testModeEnabled,
        int testModeRedirectCount,
        Instant ultimoEnvioUtc,
        String ultimoError
) {
    /** Estado sin informacion de ultimo envio ni ultimo error. */
    public EstadoCanalNotificacionProjection(String provider, String sender, boolean dispatcherEnabled, boolean disponible, String host,
                                             int port, String username, boolean tienePasswordConfigurado, String fromAddress,
                                             String fromName, boolean starttls, boolean ssl, int maxBcc, int ratePerMinute,
                                             int ratePerDay, boolean requireConfirmedEmail, String platformUrl,
                                             boolean testModeEnabled, int testModeRedirectCount) {
        this(provider, sender, dispatcherEnabled, disponible, host, port, username, tienePasswordConfigurado, fromAddress, fromName,
                starttls, ssl, maxBcc, ratePerMinute, ratePerDay, requireConfirmedEmail, platformUrl, testModeEnabled,
                testModeRedirectCount, null, null);
    }
}
