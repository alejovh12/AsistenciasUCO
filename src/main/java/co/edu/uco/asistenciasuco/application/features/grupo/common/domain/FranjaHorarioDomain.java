package co.edu.uco.asistenciasuco.application.features.grupo.common.domain;

import java.time.LocalTime;

/**
 * Franja del horario semanal de un grupo (F1): dia (codigo de dbo.Dia) y horas academicas LOCALES.
 * Las reglas de traslape y de cruce del docente las aplica la DB (usp_guardar_horarios_grupo).
 */
public record FranjaHorarioDomain(
        String codigoDia,
        String nombreDia,
        LocalTime horaInicio,
        LocalTime horaFin
) {
}
