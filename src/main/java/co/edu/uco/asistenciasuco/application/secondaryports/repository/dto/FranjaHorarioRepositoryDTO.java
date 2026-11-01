package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.time.LocalTime;

/**
 * Franja del horario semanal que se envia a usp_guardar_horarios_grupo (F1).
 */
public record FranjaHorarioRepositoryDTO(
        String codigoDia,
        LocalTime horaInicio,
        LocalTime horaFin
) {
}
