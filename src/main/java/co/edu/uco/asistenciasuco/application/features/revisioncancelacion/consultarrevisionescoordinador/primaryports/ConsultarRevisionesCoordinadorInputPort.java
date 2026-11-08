package co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionescoordinador.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.PaginaRevisiones;

import java.util.UUID;

/** CULM-001 C.1: bandeja de revisiones de los estudiantes de los programas del coordinador (ambito dentro del SP). */
public interface ConsultarRevisionesCoordinadorInputPort
        extends InteractorWithReturn<ConsultarRevisionesCoordinadorInputPort.Comando, PaginaRevisiones> {

    record Comando(UUID usuarioId, Integer pagina, Integer tamano, boolean soloPendientes, String estado, UUID programaId, UUID grupoId) {
    }
}
