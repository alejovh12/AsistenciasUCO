package co.edu.uco.asistenciasuco.application.features.docente.consultarmatrizasistencia.primaryports;

import co.edu.uco.asistenciasuco.application.features.docente.common.dto.MatrizAsistenciaGrupoDTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/**
 * CIERRE-W3M (punto 1): matriz de asistencia del grupo del docente titular, paginada por estudiante, con busqueda opcional y
 * las sesiones como encabezado. La cruza la base; el cliente solo la pinta.
 */
public interface ConsultarMatrizAsistenciaGrupoInputPort
        extends InteractorWithReturn<ConsultarMatrizAsistenciaGrupoInputPort.Consulta, MatrizAsistenciaGrupoDTO> {

    /** Entrada: grupo, paginacion y busqueda opcionales y el ejecutor (usuario autenticado). */
    record Consulta(UUID grupoId, Integer pagina, Integer tamano, String busqueda, UUID usuarioEjecutor) {
    }
}
