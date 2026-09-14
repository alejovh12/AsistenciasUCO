package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.LocalTime;
import java.util.UUID;

public record HorarioDocenteProjection(
        UUID id,
        UUID idDocente,
        UUID idGrupo,
        String codigoMateria,
        String nombreMateria,
        String seccion,
        String dia,
        LocalTime horaInicio,
        LocalTime horaFin,
        String aula,
        Integer totalEstudiantes
) {
}
