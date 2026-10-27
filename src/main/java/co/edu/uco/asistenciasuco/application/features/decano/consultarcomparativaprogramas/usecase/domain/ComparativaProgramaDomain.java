package co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record ComparativaProgramaDomain(
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
