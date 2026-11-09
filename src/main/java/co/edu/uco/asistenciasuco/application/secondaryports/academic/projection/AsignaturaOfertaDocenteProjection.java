package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

public record AsignaturaOfertaDocenteProjection(
        UUID idAsignatura,
        String codigoAsignatura,
        String nombreAsignatura,
        java.util.List<String> programas   // CULM-001 B2NM: nombres de los programas del docente que incluyen la asignatura
) {
}
