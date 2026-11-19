package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler;

import co.edu.uco.asistenciasuco.application.features.festivos.cargarfestivosautomatico.primaryports.CargarFestivosAutomaticoInputPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;

/**
 * CULM-001 B.1 (§3.4.5): job diario del SERVIDOR que carga los festivos del anio actual y de los siguientes que falten
 * (mismo patron que {@link RotacionTokenMatriculaScheduler}). Cada pasada abre su propia correlacion y nunca propaga
 * excepciones. El interruptor {@code FESTIVOS/HABILITADO} lo aplica el caso de uso. No es polling del cliente.
 */
public final class CargaFestivosScheduler extends AbstractDaemonScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CargaFestivosScheduler.class);

    private final CargarFestivosAutomaticoInputPort cargarFestivos;

    public CargaFestivosScheduler(
            final CargarFestivosAutomaticoInputPort cargarFestivos,
            final Duration retardoInicial,
            final Duration tick
    ) {
        super("carga-festivos", retardoInicial, tick);
        this.cargarFestivos = Objects.requireNonNull(cargarFestivos, "CargarFestivosAutomaticoInputPort es obligatorio.");
    }

    /** Una pasada completa; visible para pruebas. */
    @Override
    public void ejecutarPasada() {
        ejecutarConCorrelacion(() -> {
            final int cargados = cargarFestivos.execute();
            if (cargados > 0) {
                LOGGER.info("Pasada de carga de festivos completada. aniosCargados={}", cargados);
            }
        }, exception -> LOGGER.error("Fallo la pasada de carga de festivos: {}", exception.getClass().getSimpleName()));
    }
}
