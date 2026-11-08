package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.*;
import java.util.List;
import java.util.UUID;

/** Puerto secundario de asignaturas por plan: comandos y consultas sobre los SP de CULM-001 B2NM. */
public interface AsignaturaPlanPort {
    void asociar(UUID idUsuarioEjecutor, UUID idPlanEstudio, UUID idAsignatura, Integer semestreNumero, String codigoCaracter);
    void desvincular(UUID idUsuarioEjecutor, UUID idPlanEstudio, UUID idAsignatura);
    PaginaAsignaturasAsociablesProjection consultarAsociables(UUID idUsuarioEjecutor, UUID idPlanEstudio, String busqueda, int pagina, int tamano);
    List<CaracterAsignaturaProjection> consultarCaracteres();
    List<ProgramaGrupoProjection> consultarProgramasGrupo(UUID idUsuarioEjecutor, UUID idGrupo);
}
