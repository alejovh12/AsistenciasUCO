package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.List;

/** Pagina del directorio de estudiantes de los programas del coordinador. */
public record PaginaEstudiantesProgramaProjection(List<EstudianteProgramaProjection> elementos, long total) {
}
