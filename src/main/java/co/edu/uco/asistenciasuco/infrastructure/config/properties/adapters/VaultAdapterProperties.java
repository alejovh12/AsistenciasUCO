package co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

@ConfigurationProperties(prefix = "app.adapters.vault")
public record VaultAdapterProperties(Provider provider) {

    public VaultAdapterProperties {
        Objects.requireNonNull(provider, "app.adapters.vault.provider es obligatorio.");
    }

    public enum Provider {
        AZURE_KEYVAULT,
        LOCAL_ENV
    }
}
