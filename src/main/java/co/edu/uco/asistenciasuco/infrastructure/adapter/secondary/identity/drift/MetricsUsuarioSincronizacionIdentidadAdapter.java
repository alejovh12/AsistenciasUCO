package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.identity.drift;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.UsuarioSincronizacionIdentidadPort;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

/**
 * Registra un desfase DB-IdP como log estructurado (sin datos personales) y como la metrica
 * {@code identity_drift_total{operation}}. La correlacion sale del contexto de la solicitud. La persistencia en
 * tabla y la reconciliacion son de IAM-001.
 */
public final class MetricsUsuarioSincronizacionIdentidadAdapter implements UsuarioSincronizacionIdentidadPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsUsuarioSincronizacionIdentidadAdapter.class);
    static final String METRIC_NAME = "identity_drift_total";

    private final MeterRegistry meterRegistry;

    public MetricsUsuarioSincronizacionIdentidadAdapter(final MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "MeterRegistry es obligatorio.");
    }

    @Override
    public void registrar(final UUID idUsuario, final String operacion, final String errorTecnico) {
        final String operation = operacion == null ? "DESCONOCIDA" : operacion;
        meterRegistry.counter(METRIC_NAME, "operation", operation).increment();
        LOGGER.atWarn()
                .addKeyValue("eventType", "IDENTITY_DRIFT")
                .addKeyValue("operation", operation)
                .addKeyValue("userId", idUsuario)
                .addKeyValue("correlationId", CorrelationIdContext.getAsString())
                .addKeyValue("technicalError", errorTecnico)
                .log("La cuenta de identidad quedo desincronizada con la base de datos.");
    }
}
