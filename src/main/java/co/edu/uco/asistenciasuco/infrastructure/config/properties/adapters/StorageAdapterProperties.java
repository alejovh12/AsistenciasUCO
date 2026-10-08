package co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

/**
 * LB-004B.2: el unico provider soportado es MinIO (decision humana, ver
 * docs/work-items/LB-004-stateless-serverless-readiness/PROFESSOR_DECISION.md). No existe
 * fallback a filesystem local: los bytes de soporte no deben persistir en la replica backend.
 * Un valor no soportado falla el arranque (fail-closed), nunca cae silenciosamente a otro
 * provider.
 */
@ConfigurationProperties(prefix = "app.adapters.storage")
public record StorageAdapterProperties(Provider provider) {

    public StorageAdapterProperties {
        Objects.requireNonNull(provider, "app.adapters.storage.provider es obligatorio.");
    }

    public enum Provider {
        MINIO
    }
}
