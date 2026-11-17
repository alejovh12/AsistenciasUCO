package co.edu.uco.asistenciasuco.application.features.docente.consultarresumengrupos.primaryports;

import co.edu.uco.asistenciasuco.application.features.docente.common.dto.ResumenGruposDocenteDTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/**
 * CIERRE-W3I: totales de los grupos habilitados del docente (grupos, estudiantes activos, cupo y ocupacion) para su tablero y su
 * lista de grupos. Los calcula la base; el cliente no suma.
 */
public interface ConsultarResumenGruposDocenteInputPort
        extends InteractorWithReturn<ConsultarResumenGruposDocenteInputPort.Consulta, ResumenGruposDocenteDTO> {

    /** Entrada: el ejecutor (usuario autenticado). */
    record Consulta(UUID usuarioEjecutor) {
    }
}
