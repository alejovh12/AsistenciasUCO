package co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.primaryports;

import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.AudienciaRealtimeGrupo;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/** CULM-001 C.1: resuelve, una vez al suscribirse, las audiencias del usuario sobre el grupo (titular, estudiante activo, ambito de lectura). */
public interface ResolverAudienciaRealtimeInputPort
        extends InteractorWithReturn<ResolverAudienciaRealtimeInputPort.Comando, AudienciaRealtimeGrupo> {

    record Comando(UUID usuarioId, UUID grupoId) {
    }
}
