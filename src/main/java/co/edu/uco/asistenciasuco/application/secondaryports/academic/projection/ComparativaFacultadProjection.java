package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.math.BigDecimal;
import java.util.UUID;

public record ComparativaFacultadProjection(
        UUID idFacultad,
        String nombreFacultad,
        boolean estaActivaFacultad,
        int totalProgramas,
        int totalEstudiantes,
        int estudiantesRiesgo,
        int estudiantesPerdida,
        BigDecimal tasaRetencion,
        int sesionesEjecutadas,
        int faltasSinJustaCausa,
        BigDecimal porcentajeAusentismo
) {
}
