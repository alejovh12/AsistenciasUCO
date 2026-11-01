package co.edu.uco.asistenciasuco.application.secondaryports.repository.projection;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Franja del horario semanal de un grupo leida desde dbo.uv_horario (F1).
 */
public record FranjaHorarioRepositoryProjection(
        UUID idGrupo,
        String codigoDia,
        String nombreDia,
        LocalTime horaInicio,
        LocalTime horaFin
) {
}
