package co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.usecase.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record KpisCoordinadorDomain(
        UUID idPrograma,
        String nombrePrograma,
        UUID idPeriodoAcademico,
        String nombrePeriodoAcademico,
        int totalEstudiantes,
        int estudiantesRegulares,
        int estudiantesRiesgo,
        int estudiantesPerdida,
        BigDecimal tasaPermanencia,
        int sesionesProgramadas,
        int sesionesVencidas,
        int sesionesEjecutadas,
        int sesionesVencidasSinRegistro,
        int sesionesSinProcedenciaTemporal,
        BigDecimal porcentajeCumplimiento,
        BigDecimal porcentajeProgresoCalendario,
        BigDecimal umbralAdvertencia,
        BigDecimal umbralPerdida
) {
}
