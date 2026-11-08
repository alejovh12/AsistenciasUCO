package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.EstudianteProgramaProjection;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaEstudiantesProgramaProjection;
import java.util.List;
import java.util.UUID;

public interface EstudianteProgramaQueryPort {

    List<EstudianteProgramaProjection> consultarEstudiantesPorPrograma(UUID idPrograma);

    /** CULM-001 B2NM (O2-B2): directorio paginado de los estudiantes de los programas del coordinador (usp_consultar_directorio_estudiantes_paginado). */
    PaginaEstudiantesProgramaProjection consultarDirectorioPrograma(UUID actorUsuarioId, UUID idPrograma, String busqueda, Boolean estaActivo, int pagina, int tamano);
}
