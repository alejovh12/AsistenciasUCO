package co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.primaryports.dto;

import java.util.List;
import java.util.UUID;

/**
 * HU018: plan de estudios activo de un programa del estudiante, con su estructura por semestres. {@code totalSemestres} es
 * el semestre mas alto del plan (la misma regla del coordinador y del decano); los demas totales cuentan las asignaturas
 * que el estudiante ve. {@code asignaturasCursando} y {@code creditosCursando} resumen lo que cursa hoy.
 */
public record PlanEstudioEstudianteDTO(UUID idPlanEstudio, Integer inp, UUID idPrograma, String nombrePrograma,
                                       int totalSemestres, int totalAsignaturas, int totalCreditos,
                                       int asignaturasCursando, int creditosCursando,
                                       List<SemestrePlanEstudianteDTO> semestres) {
}
