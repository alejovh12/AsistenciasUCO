package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.math.BigDecimal;
import java.util.UUID;

public record ComparativaProgramaProjection(
        UUID idPrograma,
        String nombrePrograma,
        boolean estaActivoPrograma,
        int totalEstudiantes,
        int estudiantesRiesgo,
        int estudiantesPerdida,
        BigDecimal tasaRetencion,
        int sesionesEjecutadas,
        int faltasSinJustaCausa,
        BigDecimal porcentajeAusentismo
) {
}
