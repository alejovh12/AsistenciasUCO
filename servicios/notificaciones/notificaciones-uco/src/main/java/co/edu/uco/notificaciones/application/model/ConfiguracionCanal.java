package co.edu.uco.notificaciones.application.model;

import java.util.List;

/** Configuracion del canal de correo (sin el secreto: solo si hay clave configurada). */
public record ConfiguracionCanal(
        String canal,
        String host,
        int port,
        String username,
        boolean tienePassword,
        String fromAddress,
        String fromName,
        boolean starttls,
        boolean ssl,
        int maxBcc,
        int ratePerMinute,
        int ratePerDay,
        String platformUrl,
        boolean testModeEnabled,
        List<String> testModeRedirectTo
) {

    public static final String CANAL_SMTP = "smtp";
    public static final String CANAL_LOG = "log";

    public ConfiguracionCanal {
        testModeRedirectTo = testModeRedirectTo == null ? List.of() : List.copyOf(testModeRedirectTo);
    }
}
