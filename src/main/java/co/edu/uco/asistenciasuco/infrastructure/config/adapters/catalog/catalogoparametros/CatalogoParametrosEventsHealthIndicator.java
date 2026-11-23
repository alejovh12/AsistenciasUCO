package co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.catalogoparametros;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.messaging.catalogoparametros.CatalogoParametrosEventListener;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

import java.util.Objects;

/**
 * INT-004: salud de la suscripcion a los eventos del Catalogo de Parametros ({@code /actuator/health}, componente
 * {@code catalogoParametrosEventos}). Suscrito = {@code UP}. Sin suscripcion (Pulsar caido o aun arrancando) = {@code UNKNOWN}, no
 * {@code DOWN}: el backend sigue sirviendo con la cache y la BD, asi que no debe sacar la instancia del balanceador ni tumbar el
 * readiness. Los detalles nunca incluyen mensajes del broker.
 */
public final class CatalogoParametrosEventsHealthIndicator implements HealthIndicator {

    private final CatalogoParametrosEventListener listener;

    public CatalogoParametrosEventsHealthIndicator(final CatalogoParametrosEventListener listener) {
        this.listener = Objects.requireNonNull(listener, "El listener es obligatorio.");
    }

    @Override
    public Health health() {
        final Health.Builder estado = listener.estaSuscrito() ? Health.up() : Health.unknown();
        return estado
                .withDetail("suscrito", listener.estaSuscrito())
                .withDetail("ultimaSincronizacionEpochSegundos", listener.ultimaSincronizacionEpochSegundos())
                .build();
    }
}
