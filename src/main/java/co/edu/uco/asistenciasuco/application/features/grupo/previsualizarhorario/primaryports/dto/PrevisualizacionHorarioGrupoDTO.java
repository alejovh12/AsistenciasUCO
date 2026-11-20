package co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.primaryports.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SES-GEN-01: lo que el coordinador ve antes de confirmar el horario de un grupo. Los tiempos van en minutos enteros ({@code minutos*}); una
 * "hora" de la materia son 60 minutos de reloj. Las fechas del periodo y de cada sesion son del calendario institucional (yyyy-MM-dd) y los
 * instantes de las sesiones van en UTC.
 */
public record PrevisualizacionHorarioGrupoDTO(
        UUID idGrupo,
        Asignatura asignatura,
        Periodo periodo,
        String zonaHoraria,
        List<Franja> franjas,
        ResumenSemanal resumenSemanal,
        ResumenPeriodo resumenPeriodo,
        List<Festivo> festivos,
        List<Sesion> sesiones,
        CambioSobreGrupoExistente cambioSobreGrupoExistente,
        List<Conflicto> conflictos,
        boolean puedeConfirmar
) {

    /** {@code horasSemanales} nulo = la materia aun no tiene horas cargadas (no se valida el total). */
    public record Asignatura(UUID id, String codigo, String nombre, Integer horasSemanales) {
    }

    public record Periodo(UUID id, String nombre, LocalDate fechaInicio, LocalDate fechaFin, int diasPeriodo, int semanasPeriodo, String estado) {
    }

    public record Franja(String codigoDia, String nombreDia, String horaInicio, String horaFin, int minutos) {
    }

    /** Los campos de la materia son nulos si la materia no tiene horas cargadas. */
    public record ResumenSemanal(int franjasPorSemana, int minutosSemanales, Integer minutosSemanalesMateria, Integer diferenciaMinutos,
                                 Boolean coincideConMateria) {
    }

    /** {@code minutosEsperadosMateria} = horasSemanales x 60 x dias del periodo / 7; nulo si la materia no tiene horas. Informa, no bloquea. */
    public record ResumenPeriodo(int sesionesCalendario, int sesionesProgramadas, int sesionesEnFestivo, int sesionesNoProgramables,
                                 int minutosCalendario, int minutosProgramados, int minutosPerdidosFestivo, int minutosNoProgramables,
                                 Integer minutosEsperadosMateria, Integer diferenciaMinutos, Boolean cubreTotalEsperado) {
    }

    public record Festivo(LocalDate fecha, String nombre, int sesionesAfectadas) {
    }

    /** {@code estado}: PROGRAMADA, CANCELADA_POR_FESTIVO o NO_PROGRAMABLE_POR_FECHA (ya inicio: el pasado no se genera). */
    public record Sesion(int numero, int numeroSemana, LocalDate fecha, String horaInicio, String horaFin, Instant fechaHoraInicioUtc,
                         Instant fechaHoraFinUtc, int minutos, String estado, String nombreFestivo) {
    }

    /** Solo al editar un grupo existente: lo ya dictado, con asistencia, toma o clasificacion, o creado a mano, se conserva. */
    public record CambioSobreGrupoExistente(int sesionesConservadas, int minutosConservados, int sesionesFuturasQueSeRetiran,
                                            int sesionesFuturasQueSeCrean, int sesionesFuturasQueSeConservan) {
    }

    /** {@code severidad}: BLOQUEANTE (la confirmacion lo rechazaria con el mismo codigo), ADVERTENCIA o INFORMATIVO. */
    public record Conflicto(String codigo, String severidad, String mensaje, String franja) {
    }
}
