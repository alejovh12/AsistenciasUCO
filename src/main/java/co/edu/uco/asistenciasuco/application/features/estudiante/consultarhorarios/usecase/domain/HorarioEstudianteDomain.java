package co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.domain;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Modelo de resultado interno de la consulta de horarios del estudiante.
 */
public record HorarioEstudianteDomain(UUID id, UUID idEstudiante, UUID idGrupo, String codigoMateria,
                                      String nombreMateria, String grupo, String dia, LocalTime horaInicio,
                                      LocalTime horaFin, String aula, String docente) {
}
