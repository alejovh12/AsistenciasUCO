package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler;

import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.RotarTokensMatriculaAutomaticoInputPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;

/**
 * HU176B (D2): job del SERVIDOR que rota los tokens de matricula vencidos. No es polling del cliente: el docente recibe
 * el QR nuevo por SSE. Cada pasada abre su propia correlacion, nunca propaga excepciones y es idempotente entre
 * instancias (la DB solo rota si la ventana debe rotar). El periodo de rotacion lo decide la DB; este tick solo fija
 * cada cuanto se revisa.
 */
public final class RotacionTokenMatriculaScheduler extends AbstractDaemonScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotacionTokenMatriculaScheduler.class);

    private final RotarTokensMatriculaAutomaticoInputPort rotarTokens;

    public RotacionTokenMatriculaScheduler(
            final RotarTokensMatriculaAutomaticoInputPort rotarTokens,
            final Duration tick
    ) {
        super("rotacion-token-matricula-qr", tick, tick);
        this.rotarTokens = Objects.requireNonNull(rotarTokens, "RotarTokensMatriculaAutomaticoInputPort es obligatorio.");
    }

    /** Una pasada completa; visible para pruebas. */
    @Override
    public void ejecutarPasada() {
        ejecutarConCorrelacion(rotarTokens::execute,
                exception -> LOGGER.error("Fallo la pasada de rotacion de tokens de matricula: {}",
                        exception.getClass().getSimpleName()));
    }
}
