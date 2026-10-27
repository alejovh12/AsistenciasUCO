package co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ComparativaProgramaDTO(
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
