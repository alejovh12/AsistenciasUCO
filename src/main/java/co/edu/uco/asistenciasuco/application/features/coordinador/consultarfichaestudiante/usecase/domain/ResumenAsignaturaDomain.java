package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record ResumenAsignaturaDomain(
        UUID idGrupo,
        int codigoGrupo,
        UUID idAsignatura,
        String nombreAsignatura,
        String codigoEstadoEstudiante,
        String nombreEstadoEstudiante,
        int sesionesEjecutadas,
        int asistencias,
        int faltasSinJustaCausa,
        int excusas,
        BigDecimal porcentajeInasistencia,
        String codigoPermanencia
) {
}
