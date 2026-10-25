package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.math.BigDecimal;
import java.util.UUID;

/** Resumen de asistencia de una matricula (HU069). */
public record ResumenAsignaturaProjection(
        UUID idGrupo,
        int codigoGrupo,
        UUID idAsignatura,
        String nombreAsignatura,
        String codigoEstadoEstudiante,
        String nombreEstadoEstudiante,
        int sesionesEjecutadas,
        int asistencias,
        int faltasSinJustaCausa,
        int excusas,
        BigDecimal porcentajeInasistencia,
        String codigoPermanencia
) {
}
