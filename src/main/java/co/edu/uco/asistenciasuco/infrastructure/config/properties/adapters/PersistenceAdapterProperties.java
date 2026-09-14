package co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

@ConfigurationProperties(prefix = "app.adapters.persistence")
public record PersistenceAdapterProperties(Provider provider) {

    public PersistenceAdapterProperties {
        Objects.requireNonNull(provider, "app.adapters.persistence.provider es obligatorio.");
    }

    public enum Provider {
        SQLSERVER
    }
}
