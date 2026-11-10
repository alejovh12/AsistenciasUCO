package co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionesdocente.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.PaginaRevisiones;

import java.util.UUID;

/** DEC-065 (1), HU026C: revisiones de cancelacion de los grupos donde el usuario es docente titular (ambito dentro del SP). */
public interface ConsultarRevisionesDocenteInputPort
        extends InteractorWithReturn<ConsultarRevisionesDocenteInputPort.Comando, PaginaRevisiones> {

    record Comando(UUID usuarioId, Integer pagina, Integer tamano, boolean soloPendientes, String estado, UUID grupoId) {
    }
}
