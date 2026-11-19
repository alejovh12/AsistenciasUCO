package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SES-GEN-01: resultado de usp_previsualizar_horario_grupo (solo lectura). Todos los tiempos de la DB van en MINUTOS enteros; la
 * decision de negocio (ranuras, festivos, conflictos) es de la DB, el backend solo la traduce.
 */
public record PrevisualizacionHorarioGrupoProjection(
        Resumen resumen,
        List<Franja> franjas,
        List<Sesion> sesiones,
        List<Festivo> festivos,
        List<Conflicto> conflictos
) {

    public record Resumen(
            UUID idGrupo, UUID idAsignatura, UUID idPeriodo, String codigoAsignatura, String nombreAsignatura, String nombrePeriodo,
            String estadoPeriodo, String zonaHoraria, LocalDate fechaInicioPeriodo, LocalDate fechaFinPeriodo,
            Integer horasSemanalesMateria, int minutosSemanales, int franjasPorSemana, int diasPeriodo,
            int sesionesCalendario, int sesionesProgramadas, int sesionesEnFestivo, int sesionesNoProgramables,
            int minutosCalendario, int minutosProgramados, int minutosPerdidosFestivo, int minutosNoProgramables,
            Integer minutosEsperadosMateria,
            Integer sesionesConservadas, Integer minutosConservados, Integer sesionesFuturasQueSeRetiran,
            Integer sesionesFuturasQueSeCrean, Integer sesionesFuturasQueSeConservan,
            boolean puedeConfirmar, boolean esGrupoExistente
    ) {
    }

    public record Franja(String codigoDia, String nombreDia, String horaInicio, String horaFin, int minutos) {
    }

    public record Sesion(
            int numero, int numeroSemana, LocalDate fecha, String horaInicio, String horaFin,
            Instant fechaHoraInicioUtc, Instant fechaHoraFinUtc, int minutos, String estado, String nombreFestivo
    ) {
    }

    public record Festivo(LocalDate fecha, String nombre, int sesionesAfectadas) {
    }

    /** {@code severidad}: BLOQUEANTE (la confirmacion lo rechazaria), ADVERTENCIA o INFORMATIVO. El mensaje sale del catalogo de la DB. */
    public record Conflicto(String codigo, String severidad, String mensaje, String franja) {
    }
}
