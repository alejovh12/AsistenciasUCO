package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.LocalTime;
import java.util.UUID;

public record HorarioEstudianteProjection(
        UUID id,
        UUID idEstudiante,
        UUID idGrupo,
        String codigoMateria,
        String nombreMateria,
        String grupo,
        String dia,
        LocalTime horaInicio,
        LocalTime horaFin,
        String aula,
        String docente
) {
}
