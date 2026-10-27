package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.composite;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Adaptador compuesto de {@link NotificacionPort} que realiza fan-out aislado a multiples destinos
 * configurados en orden. El fallo de un destino no impide la invocacion de los demas.
 */
public class CompositeNotificacionAdapter implements NotificacionPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeNotificacionAdapter.class);

    public record NamedTarget(String nombre, NotificacionPort puerto) {
        public NamedTarget {
            Objects.requireNonNull(nombre, "El nombre del target es obligatorio.");
            Objects.requireNonNull(puerto, "El puerto del target es obligatorio.");
        }
    }

    private final List<NamedTarget> targets;
    private final MeterRegistry meterRegistry;

    public CompositeNotificacionAdapter(
            final List<NamedTarget> targets,
            final MeterRegistry meterRegistry
    ) {
        Objects.requireNonNull(targets, "La lista de targets es obligatoria.");
        this.targets = List.copyOf(targets);
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "MeterRegistry es obligatorio.");
    }

    @Override
    public void notificar(final Notificacion notificacion) {
        Objects.requireNonNull(notificacion, "La notificacion es obligatoria.");

        for (final NamedTarget target : targets) {
            try {
                target.puerto().notificar(notificacion);
            } catch (final RuntimeException e) {
                LOGGER.warn("Fallo al entregar notificacion en destino composite {}: {}", target.nombre(), e.getClass().getSimpleName());
                Counter.builder("notification.composite.target.failures")
                        .tag("target", target.nombre())
                        .register(meterRegistry)
                        .increment();
            }
        }
    }
}
