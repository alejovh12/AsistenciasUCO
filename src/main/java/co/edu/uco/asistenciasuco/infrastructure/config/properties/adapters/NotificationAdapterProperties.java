package co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Propiedades de seleccion para el adaptador de notificaciones.
 */
@ConfigurationProperties(prefix = "app.adapters.notification")
public record NotificationAdapterProperties(
        @DefaultValue("LOG") Provider provider,
        Composite composite
) {

    @ConstructorBinding
    public NotificationAdapterProperties {
        Objects.requireNonNull(provider, "app.adapters.notification.provider es obligatorio.");
        if (provider == Provider.COMPOSITE) {
            if (composite == null || composite.targets() == null || composite.targets().size() < 2) {
                throw new IllegalArgumentException("app.adapters.notification.composite.targets debe contener al menos dos destinos.");
            }
            final Set<Target> distinctTargets = new HashSet<>(composite.targets());
            if (distinctTargets.size() != composite.targets().size()) {
                throw new IllegalArgumentException("app.adapters.notification.composite.targets no debe contener destinos repetidos.");
            }
        }
    }

    public enum Provider {
        LOG,
        OUTBOX,
        COMPOSITE
    }

    public enum Target {
        LOG,
        OUTBOX
    }

    public record Composite(List<Target> targets) {
        public Composite {
            targets = targets == null ? List.of() : List.copyOf(targets);
        }
    }
}
