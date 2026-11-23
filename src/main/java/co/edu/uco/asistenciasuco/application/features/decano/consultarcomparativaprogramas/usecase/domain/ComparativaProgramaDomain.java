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
        BigDecimal porcentajeAusentismo,
        BigDecimal horasDictadas,
        BigDecimal horasFalta,
        BigDecimal horasPeriodo
) {

    /** Forma previa a la revision final P3 (BD-7): sin horasPeriodo (nulo). */
    public ComparativaProgramaDomain(final UUID idPrograma, final String nombrePrograma, final boolean estaActivoPrograma, final int totalEstudiantes, final int estudiantesRiesgo, final int estudiantesPerdida, final BigDecimal tasaRetencion, final int sesionesEjecutadas, final int faltasSinJustaCausa, final BigDecimal porcentajeAusentismo, final BigDecimal horasDictadas, final BigDecimal horasFalta) {
        this(idPrograma, nombrePrograma, estaActivoPrograma, totalEstudiantes, estudiantesRiesgo, estudiantesPerdida, tasaRetencion, sesionesEjecutadas, faltasSinJustaCausa, porcentajeAusentismo, horasDictadas, horasFalta, null);
    }
}
