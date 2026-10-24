package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.math.BigDecimal;
import java.util.UUID;

/** Cifras de la facultad tal como las calcula la DB. */
public record KpisDecanoProjection(
        UUID idFacultad,
        String nombreFacultad,
        UUID idPeriodoAcademico,
        String nombrePeriodoAcademico,
        int totalProgramas,
        int totalEstudiantes,
        int estudiantesRegulares,
        int estudiantesRiesgo,
        int estudiantesPerdida,
        BigDecimal tasaRetencion,
        int sesionesEjecutadas,
        int faltasSinJustaCausa,
        BigDecimal porcentajeAusentismo,
        BigDecimal porcentajeAsistencia,
        int gruposActivos,
        int cupoTotal,
        int matriculadosActivos,
        BigDecimal tasaAforo,
        BigDecimal umbralAdvertencia,
        BigDecimal umbralPerdida
) {
}
