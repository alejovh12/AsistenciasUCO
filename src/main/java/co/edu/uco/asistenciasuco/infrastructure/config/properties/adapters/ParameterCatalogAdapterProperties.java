package co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

@ConfigurationProperties(prefix = "app.adapters.parameter-catalog")
public record ParameterCatalogAdapterProperties(Provider provider) {

    public ParameterCatalogAdapterProperties {
        Objects.requireNonNull(provider, "app.adapters.parameter-catalog.provider es obligatorio.");
    }

    public enum Provider {
        AZURE_APPCONFIG,
        SQLSERVER
    }
}
