package co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.usecase;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.*;
import java.util.List;
import java.util.UUID;

/** Caso de uso de asignaturas por plan (CULM-001 B2NM, DEC-019). */
public interface AsignaturaPlanUseCase {
    void asociar(UUID idUsuarioEjecutor, UUID idPlanEstudio, UUID idAsignatura, Integer semestreNumero, String codigoCaracter);
    void desvincular(UUID idUsuarioEjecutor, UUID idPlanEstudio, UUID idAsignatura);
    PaginaAsignaturasAsociablesProjection consultarAsociables(UUID idUsuarioEjecutor, UUID idPlanEstudio, String busqueda, int pagina, int tamano);
    List<CaracterAsignaturaProjection> consultarCaracteres();
    List<ProgramaGrupoProjection> consultarProgramasGrupo(UUID idUsuarioEjecutor, UUID idGrupo);
    PaginaAsignaturasPlanProjection consultarMalla(UUID idUsuarioEjecutor, UUID idPlanEstudio, Integer numeroSemestre, String codigoCaracter, String busqueda, int pagina, int tamano);
    PaginaAsignaturasPlanProjection consultarAsignaturasCoordinador(UUID idUsuarioEjecutor, UUID idPrograma, String busqueda, Boolean estaActiva, int pagina, int tamano);
    PaginaPlanesEstudioProjection consultarPlanes(UUID idUsuarioEjecutor, UUID idPrograma, Boolean estaActivo, int pagina, int tamano);
    PaginaGruposFacultadProjection consultarGruposFacultad(UUID idUsuarioEjecutor, UUID idPeriodoAcademico, UUID idPrograma, String busqueda, int pagina, int tamano);
}
