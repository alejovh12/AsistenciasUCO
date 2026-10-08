package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuracion para la conexion con MinIO. {@code accessKey}/{@code secretKey}
 * se resuelven exclusivamente desde variables de entorno (sin valor por defecto funcional);
 * cero secretos hardcoded.
 */
@ConfigurationProperties(prefix = "app.providers.minio")
public record MinioProviderProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket
) {
    public MinioProviderProperties {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("app.providers.minio.endpoint es obligatorio.");
        }
        if (accessKey == null || accessKey.isBlank()) {
            throw new IllegalStateException("app.providers.minio.access-key es obligatorio.");
        }
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("app.providers.minio.secret-key es obligatorio.");
        }
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("app.providers.minio.bucket es obligatorio.");
        }
    }
}
