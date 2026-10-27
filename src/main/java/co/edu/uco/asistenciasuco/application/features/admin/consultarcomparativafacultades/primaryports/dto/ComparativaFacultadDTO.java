package co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ComparativaFacultadDTO(
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
