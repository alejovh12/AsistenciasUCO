package co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase.domain;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Modelo de resultado interno de la consulta de horarios del docente.
 */
public record HorarioDocenteDomain(UUID id, UUID idDocente, UUID idGrupo, String codigoMateria, String nombreMateria,
                                   String seccion, String dia, LocalTime horaInicio, LocalTime horaFin,
                                   String aula, Integer totalEstudiantes) {
}
