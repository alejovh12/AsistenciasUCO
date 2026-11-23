package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.ActualizarSemestreEstudianteCommand;

public interface EstudianteSemestreCommandPort {

    /** Ejecuta usp_actualizar_semestre_estudiante_programa y devuelve el mensaje de usuario del catalogo (EST_007). */
    String actualizarSemestre(ActualizarSemestreEstudianteCommand command);
}
