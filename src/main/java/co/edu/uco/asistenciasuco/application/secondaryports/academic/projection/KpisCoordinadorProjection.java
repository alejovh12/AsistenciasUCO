package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.math.BigDecimal;
import java.util.UUID;

/** Fila de usp_consultar_kpis_coordinador: cifras calculadas en DB (HU065, HU070). */
public record KpisCoordinadorProjection(
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
