package co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record KpisInstitucionDomain(
        UUID idInstitucion,
        String nombreInstitucion,
        UUID idPeriodoAcademico,
        String nombrePeriodoAcademico,
        int totalFacultades,
        int totalProgramas,
        int totalEstudiantes,
        int estudiantesRegulares,
        int estudiantesRiesgo,
        int estudiantesPerdida,
        BigDecimal tasaRetencion,
        int sesionesEjecutadas,
        int faltasSinJustaCausa,
        BigDecimal porcentajeAusentismo,
        BigDecimal porcentajeAsistencia,
        int sesionesVencidas,
        int sesionesConRegistro,
        BigDecimal porcentajeCumplimiento,
        int estudiantesConMatriculaActiva,
        int estudiantesRegistrados,
        int docentesConGrupoActivo,
        int docentesRegistrados,
        BigDecimal umbralAdvertencia,
        BigDecimal umbralPerdida
) {
}
