package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.function.ToIntFunction;

/**
 * Job diario del SERVIDOR que purga {@code dbo.AuditoriaEvento}. Usa el ciclo de vida compartido de los jobs
 * periodicos del servidor. La auditoria conserva su contrato aparte del resto de la arquitectura (ver
 * docs/observability/README.md), asi que depende directamente del repositorio JPA de auditoria en vez de un puerto
 * de Application: no hay caso de uso de negocio detras de una purga de retencion.
 *
 * <p>Cada pasada abre su propia correlacion y recalcula el limite ({@code ahora - retencion}) para que la retencion sea
 * siempre relativa al momento de la purga.</p>
 */
public final class PurgaAuditoriaScheduler extends AbstractDaemonScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PurgaAuditoriaScheduler.class);

    private final ToIntFunction<OffsetDateTime> purgarAnterioresA;
    private final Clock clock;
    private final Duration retencion;

    public PurgaAuditoriaScheduler(
            final ToIntFunction<OffsetDateTime> purgarAnterioresA,
            final Clock clock,
            final Duration retencion,
            final Duration retardoInicial,
            final Duration tick
    ) {
        super("purga-auditoria", retardoInicial, tick);
        this.purgarAnterioresA = Objects.requireNonNull(purgarAnterioresA, "La operacion de purga es obligatoria.");
        this.clock = Objects.requireNonNull(clock, "El reloj es obligatorio.");
        this.retencion = Objects.requireNonNull(retencion, "La retencion de auditoria es obligatoria.");
        if (retencion.isZero() || retencion.isNegative()) {
            throw new IllegalArgumentException("La retencion de auditoria debe ser positiva.");
        }
    }

    /** Una pasada completa; visible para pruebas. */
    @Override
    public void ejecutarPasada() {
        ejecutarConCorrelacion(() -> {
            final OffsetDateTime limite = OffsetDateTime.now(clock).minus(retencion);
            final int eliminados = purgarAnterioresA.applyAsInt(limite);
            if (eliminados > 0) {
                LOGGER.info("Pasada de purga de auditoria completada. eliminados={}, limite={}", eliminados, limite);
            }
        }, exception -> LOGGER.error("Fallo la pasada de purga de auditoria.", exception));
    }
}
