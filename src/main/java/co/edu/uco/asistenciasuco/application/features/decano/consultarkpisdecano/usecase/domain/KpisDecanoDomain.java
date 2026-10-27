package co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record KpisDecanoDomain(
        UUID idFacultad,
        String nombreFacultad,
        UUID idPeriodoAcademico,
        String nombrePeriodoAcademico,
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
        int gruposActivos,
        int cupoTotal,
        int matriculadosActivos,
        BigDecimal tasaAforo,
        BigDecimal umbralAdvertencia,
        BigDecimal umbralPerdida
) {
}
