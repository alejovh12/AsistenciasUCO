package co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.dto;

import java.time.LocalTime;
import java.util.UUID;

public record HorarioDocenteDTO(UUID id, UUID idDocente, UUID idGrupo, String codigoMateria, String nombreMateria,
                                String seccion, String dia, LocalTime horaInicio, LocalTime horaFin,
                                Integer totalEstudiantes) {
}
