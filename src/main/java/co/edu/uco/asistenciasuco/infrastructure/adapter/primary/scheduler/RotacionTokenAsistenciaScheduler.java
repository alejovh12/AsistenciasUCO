package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler;

import co.edu.uco.asistenciasuco.application.features.asistenciaqr.rotartokensasistenciaautomatico.primaryports.RotarTokensAsistenciaAutomaticoInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.avisarfaltassesion.primaryports.AvisarFaltasSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartomasvencidas.primaryports.CerrarTomasVencidasInputPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;

/**
 * CULM-001 B.2 (contrato 3.5): job del SERVIDOR (patron de {@link RotacionTokenMatriculaScheduler}) que (1) rota los
 * tokens de asistencia vencidos y empuja el QR nuevo al titular por SSE, (2) cierra las tomas cuya ventana vencio y
 * (3) ATA D1: avisa por correo las faltas de las sesiones que ya terminaron (despues del cierre, para que las tomas
 * recien cerradas ya cuenten). No es polling del cliente. Cada pasada abre su propia correlacion, nunca propaga
 * excepciones y es segura entre instancias (la base de datos decide si rota o cierra).
 */
public final class RotacionTokenAsistenciaScheduler extends AbstractDaemonScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotacionTokenAsistenciaScheduler.class);

    private final RotarTokensAsistenciaAutomaticoInputPort rotarTokens;
    private final CerrarTomasVencidasInputPort cerrarVencidas;
    private final AvisarFaltasSesionInputPort avisarFaltas;

    public RotacionTokenAsistenciaScheduler(
            final RotarTokensAsistenciaAutomaticoInputPort rotarTokens,
            final CerrarTomasVencidasInputPort cerrarVencidas,
            final AvisarFaltasSesionInputPort avisarFaltas,
            final Duration tick
    ) {
        super("rotacion-token-asistencia-qr", tick, tick);
        this.rotarTokens = Objects.requireNonNull(rotarTokens, "RotarTokensAsistenciaAutomaticoInputPort es obligatorio.");
        this.cerrarVencidas = Objects.requireNonNull(cerrarVencidas, "CerrarTomasVencidasInputPort es obligatorio.");
        this.avisarFaltas = Objects.requireNonNull(avisarFaltas, "AvisarFaltasSesionInputPort es obligatorio.");
    }

    /** Una pasada completa (rotacion, cierre y aviso de faltas); visible para pruebas. */
    @Override
    public void ejecutarPasada() {
        ejecutarConCorrelacion(() -> {
            ejecutar("rotacion de tokens de asistencia", rotarTokens::execute);
            ejecutar("cierre de tomas vencidas", cerrarVencidas::execute);
            ejecutar("aviso de faltas por sesion", avisarFaltas::execute);
        }, exception -> LOGGER.error("Fallo una pasada de rotacion de tokens de asistencia.", exception));
    }

    private static void ejecutar(final String nombre, final Runnable paso) {
        try {
            paso.run();
        } catch (final RuntimeException exception) {
            LOGGER.error("Fallo la pasada de {}: {}", nombre, exception.getClass().getSimpleName());
        }
    }
}
