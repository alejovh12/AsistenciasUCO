package co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

@ConfigurationProperties(prefix = "app.adapters.identity")
public record IdentityAdapterProperties(Provider provider) {

    public IdentityAdapterProperties {
        Objects.requireNonNull(provider, "app.adapters.identity.provider es obligatorio.");
    }

    public enum Provider {
        /** Compatibilidad con el valor histórico; equivale a {@link #LEGACY}. */
        KEYCLOAK,
        /** Keycloak legacy (puerto 8081, realm asistencias-uco). */
        LEGACY,
        /** Identidad central de Security Baseline (realm security-baseline). */
        BASELINE
    }
}
