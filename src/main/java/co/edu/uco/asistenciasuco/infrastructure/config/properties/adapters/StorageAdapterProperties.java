package co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

@ConfigurationProperties(prefix = "app.adapters.storage")
public record StorageAdapterProperties(Provider provider) {

    public StorageAdapterProperties {
        Objects.requireNonNull(provider, "app.adapters.storage.provider es obligatorio.");
    }

    public enum Provider {
        LOCAL
    }
}
