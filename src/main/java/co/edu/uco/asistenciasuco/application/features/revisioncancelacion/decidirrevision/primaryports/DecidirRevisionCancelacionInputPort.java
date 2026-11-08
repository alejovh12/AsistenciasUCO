package co.edu.uco.asistenciasuco.application.features.revisioncancelacion.decidirrevision.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.ResultadoDecision;

import java.util.UUID;

/** CULM-001 C.1 (HU026C): el docente titular mantiene o propone la cancelacion de un estudiante en pérdida, con motivo. */
public interface DecidirRevisionCancelacionInputPort
        extends InteractorWithReturn<DecidirRevisionCancelacionInputPort.Comando, ResultadoDecision> {

    record Comando(UUID revisionId, String decision, String motivo, UUID usuarioEjecutor) {
    }
}
