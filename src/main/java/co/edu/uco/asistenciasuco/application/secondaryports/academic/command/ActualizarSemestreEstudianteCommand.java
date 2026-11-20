package co.edu.uco.asistenciasuco.application.secondaryports.academic.command;

import java.util.UUID;

/** Semestre actual del estudiante en el programa (nulo lo limpia). */
public record ActualizarSemestreEstudianteCommand(UUID idEstudiante, UUID idPrograma, Integer semestreActual, UUID usuarioEjecutor) {
}
