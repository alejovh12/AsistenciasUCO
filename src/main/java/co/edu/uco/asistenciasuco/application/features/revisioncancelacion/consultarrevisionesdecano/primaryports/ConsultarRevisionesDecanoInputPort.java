package co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionesdecano.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.PaginaRevisiones;

import java.util.UUID;

/** CULM-001 C.1 (DEC-043): bandeja del decano con las revisiones escaladas a el o resueltas por el. */
public interface ConsultarRevisionesDecanoInputPort
        extends InteractorWithReturn<ConsultarRevisionesDecanoInputPort.Comando, PaginaRevisiones> {

    record Comando(UUID usuarioId, Integer pagina, Integer tamano, boolean soloPendientes, String estado, UUID programaId) {
    }
}
