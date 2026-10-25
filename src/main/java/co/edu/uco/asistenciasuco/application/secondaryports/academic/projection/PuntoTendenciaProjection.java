package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PuntoTendenciaProjection(
        LocalDate inicioIntervalo,
        LocalDate finIntervalo,
        int sesionesEjecutadas,
        int faltasSinJustaCausa,
        BigDecimal porcentajeAusentismo
) {
}
