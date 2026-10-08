package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuracion para el cliente clamd/INSTREAM de ClamAV.
 */
@ConfigurationProperties(prefix = "app.providers.clamav")
public record ClamAvProviderProperties(
        String host,
        int port,
        int connectTimeoutMillis,
        int readTimeoutMillis
) {
    public ClamAvProviderProperties {
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("app.providers.clamav.host es obligatorio.");
        }
        if (port <= 0) {
            throw new IllegalStateException("app.providers.clamav.port debe ser un puerto valido.");
        }
        if (connectTimeoutMillis <= 0) {
            connectTimeoutMillis = 5000;
        }
        if (readTimeoutMillis <= 0) {
            readTimeoutMillis = 15000;
        }
    }
}
