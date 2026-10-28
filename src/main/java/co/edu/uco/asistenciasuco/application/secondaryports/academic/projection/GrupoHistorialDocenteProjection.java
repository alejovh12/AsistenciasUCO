package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.LocalDate;
import java.util.UUID;

/** Fila del historial de grupos del docente (HU094). estadoPeriodo: VIGENTE, FINALIZADO o PROXIMO. */
public record GrupoHistorialDocenteProjection(
        UUID idGrupo,
        Integer codigoGrupo,
        String nombreGrupo,
        UUID idAsignatura,
        String nombreAsignatura,
        UUID idPeriodoAcademico,
        Integer codigoPeriodoAcademico,
        String nombrePeriodoAcademico,
        LocalDate fechaInicioPeriodo,
        LocalDate fechaFinPeriodo,
        String estadoPeriodo,
        int capacidadMaxima,
        int estudiantesActivos,
        int estudiantesFinalizados,
        int estudiantesCanceladosVoluntad,
        int estudiantesCanceladosInasistencia
) {
}
