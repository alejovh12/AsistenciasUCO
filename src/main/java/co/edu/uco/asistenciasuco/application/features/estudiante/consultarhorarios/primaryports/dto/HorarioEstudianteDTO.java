package co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.dto;

import java.time.LocalTime;
import java.util.UUID;

public record HorarioEstudianteDTO(UUID id, UUID idEstudiante, UUID idGrupo, String codigoMateria,
                                   String nombreMateria, String grupo, String dia, LocalTime horaInicio,
                                   LocalTime horaFin, String aula, String docente) {
}
