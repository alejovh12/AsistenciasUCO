package co.edu.uco.asistenciasuco.application.features.grupo.common.dto;

/**
 * Franja del horario semanal de un grupo (F1). {@code codigoDia} es el codigo de dbo.Dia (LU, MA, MI, JU, VI, SA, DO);
 * las horas son hora academica LOCAL en formato "HH:mm". {@code nombreDia} solo viene en las consultas.
 */
public record FranjaHorarioDTO(
        String codigoDia,
        String nombreDia,
        String horaInicio,
        String horaFin
) {

    public FranjaHorarioDTO(final String codigoDia, final String horaInicio, final String horaFin) {
        this(codigoDia, null, horaInicio, horaFin);
    }
}
