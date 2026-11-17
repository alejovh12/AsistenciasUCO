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
        BigDecimal umbralPerdida,
        int totalDocentes,
        int gruposActivos,
        int cupoTotal,
        int matriculadosActivos,
        BigDecimal tasaAforo,
        int estudiantesRegistrados
) {

    /** Forma previa a CIERRE-W3I (sin los totales nuevos: cero y nulos). */
    public KpisCoordinadorProjection(final UUID idPrograma, final String nombrePrograma, final UUID idPeriodoAcademico, final String nombrePeriodoAcademico, final int totalEstudiantes, final int estudiantesRegulares, final int estudiantesRiesgo, final int estudiantesPerdida, final BigDecimal tasaPermanencia, final int sesionesProgramadas, final int sesionesVencidas, final int sesionesEjecutadas, final int sesionesVencidasSinRegistro, final int sesionesSinProcedenciaTemporal, final BigDecimal porcentajeCumplimiento, final BigDecimal porcentajeProgresoCalendario, final BigDecimal umbralAdvertencia, final BigDecimal umbralPerdida) {
        this(idPrograma, nombrePrograma, idPeriodoAcademico, nombrePeriodoAcademico, totalEstudiantes, estudiantesRegulares, estudiantesRiesgo, estudiantesPerdida, tasaPermanencia, sesionesProgramadas, sesionesVencidas, sesionesEjecutadas, sesionesVencidasSinRegistro, sesionesSinProcedenciaTemporal, porcentajeCumplimiento, porcentajeProgresoCalendario, umbralAdvertencia, umbralPerdida, 0, 0, 0, 0, null, 0);
    }

}
