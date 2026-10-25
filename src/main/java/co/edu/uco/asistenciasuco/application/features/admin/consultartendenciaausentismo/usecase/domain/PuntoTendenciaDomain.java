package co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.usecase.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PuntoTendenciaDomain(
        LocalDate inicioIntervalo,
        LocalDate finIntervalo,
        int sesionesEjecutadas,
        int faltasSinJustaCausa,
        BigDecimal porcentajeAusentismo
) {
}
