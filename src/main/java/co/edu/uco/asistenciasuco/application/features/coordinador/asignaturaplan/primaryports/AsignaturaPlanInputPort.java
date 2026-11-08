package co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.dto.*;
import java.util.List;
import java.util.UUID;

/** Puerto de entrada de las asignaturas por plan (CULM-001 B2NM, DEC-019). */
public interface AsignaturaPlanInputPort {
    void asociar(UUID idUsuarioEjecutor, UUID idPlanEstudio, AsociarAsignaturaPlanDTO dto);
    void desvincular(UUID idUsuarioEjecutor, UUID idPlanEstudio, UUID idAsignatura);
    PaginaAsignaturasAsociablesDTO consultarAsociables(UUID idUsuarioEjecutor, UUID idPlanEstudio, String busqueda, int pagina, int tamano);
    List<CaracterAsignaturaDTO> consultarCaracteres();
    List<ProgramaResumenDTO> consultarProgramasGrupo(UUID idUsuarioEjecutor, UUID idGrupo);
}
