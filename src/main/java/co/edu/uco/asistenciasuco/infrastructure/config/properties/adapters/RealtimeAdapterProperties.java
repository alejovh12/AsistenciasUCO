package co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

@ConfigurationProperties(prefix = "app.adapters.realtime")
public record RealtimeAdapterProperties(Provider provider) {

    public RealtimeAdapterProperties {
        Objects.requireNonNull(provider, "app.adapters.realtime.provider es obligatorio.");
    }

    public enum Provider {
        LOCAL_SSE
    }
}
