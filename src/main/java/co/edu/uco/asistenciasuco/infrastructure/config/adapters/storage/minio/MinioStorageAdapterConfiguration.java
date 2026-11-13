package co.edu.uco.asistenciasuco.infrastructure.config.adapters.storage.minio;

import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.storage.minio.MinioFileStorageAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.MinioProviderProperties;
import io.minio.MinioClient;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.storage.resilience.CircuitBreakerFileStorageAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.resilience.ResilienceCircuitBreakerNames;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root para el provider de storage MinIO.
 *
 * <p>Se activa cuando {@code app.adapters.storage.provider=minio} (unico provider soportado desde
 * LB-004B.2; ver {@code StorageAdapterProperties}). Fail-fast: si falta configuracion obligatoria
 * ({@code endpoint}/{@code access-key}/{@code secret-key}/{@code bucket}), el arranque falla al
 * construir {@link MinioProviderProperties}, antes de exponer este bean. Los timeouts de
 * conexion/escritura/lectura se aplican siempre (fail-closed acotado).</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.adapters.storage",
        name = "provider",
        havingValue = "minio",
        matchIfMissing = true
)
@EnableConfigurationProperties(MinioProviderProperties.class)
public class MinioStorageAdapterConfiguration {

    @Bean
    public FileStoragePort fileStoragePort(
            final MinioProviderProperties properties,
            final ObjectProvider<CircuitBreakerRegistry> circuitBreakerRegistry
    ) {
        final MinioClient minioClient = MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
        // Sin esto el SDK espera hasta 5 minutos a un MinIO que acepta la conexion y no responde.
        minioClient.setTimeout(
                properties.connectTimeoutMillis(),
                properties.writeTimeoutMillis(),
                properties.readTimeoutMillis());

        // REA-02: breaker "fileStorage"; con el breaker abierto se falla rapido con StorageUnavailableException.
        return new CircuitBreakerFileStorageAdapter(
                new MinioFileStorageAdapter(minioClient, properties.bucket()),
                circuitBreakerRegistry.getIfAvailable(CircuitBreakerRegistry::ofDefaults)
                        .circuitBreaker(ResilienceCircuitBreakerNames.FILE_STORAGE));
    }
}
