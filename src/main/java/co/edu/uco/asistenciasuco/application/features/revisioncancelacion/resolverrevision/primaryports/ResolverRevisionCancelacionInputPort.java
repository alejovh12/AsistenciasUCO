package co.edu.uco.asistenciasuco.application.features.revisioncancelacion.resolverrevision.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.ResultadoResolucion;

import java.util.UUID;

/** CULM-001 C.1 (HU026C): el coordinador del programa del estudiante (o el decano escalado) aprueba o rechaza la propuesta. */
public interface ResolverRevisionCancelacionInputPort
        extends InteractorWithReturn<ResolverRevisionCancelacionInputPort.Comando, ResultadoResolucion> {

    record Comando(UUID revisionId, String decision, String motivo, UUID usuarioEjecutor) {
    }
}
