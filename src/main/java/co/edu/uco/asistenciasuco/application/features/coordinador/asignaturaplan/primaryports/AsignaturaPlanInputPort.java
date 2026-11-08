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
    PaginaAsignaturasPlanDTO consultarMalla(UUID idUsuarioEjecutor, UUID idPlanEstudio, Integer numeroSemestre, String codigoCaracter, String busqueda, int pagina, int tamano);
    PaginaAsignaturasPlanDTO consultarAsignaturasCoordinador(UUID idUsuarioEjecutor, UUID idPrograma, String busqueda, Boolean estaActiva, int pagina, int tamano);
    PaginaPlanesEstudioDTO consultarPlanes(UUID idUsuarioEjecutor, UUID idPrograma, Boolean estaActivo, int pagina, int tamano);
    PaginaGruposFacultadDTO consultarGruposFacultad(UUID idUsuarioEjecutor, UUID idPeriodoAcademico, UUID idPrograma, String busqueda, int pagina, int tamano);
}
