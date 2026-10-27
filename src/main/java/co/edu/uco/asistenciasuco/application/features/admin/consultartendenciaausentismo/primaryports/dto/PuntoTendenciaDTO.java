package co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PuntoTendenciaDTO(
        LocalDate inicioIntervalo,
        LocalDate finIntervalo,
        int sesionesEjecutadas,
        int faltasSinJustaCausa,
        BigDecimal porcentajeAusentismo
) {
}
