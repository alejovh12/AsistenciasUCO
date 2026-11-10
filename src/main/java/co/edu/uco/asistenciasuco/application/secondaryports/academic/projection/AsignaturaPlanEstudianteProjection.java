package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/**
 * HU018: fila plana de {@code usp_consultar_plan_estudio_estudiante}: un semestre de un plan activo del estudiante y una
 * de sus asignaturas. Un semestre sin asignaturas llega en UNA fila con los datos de la asignatura en nulo
 * ({@code idAsignatura == null}).
 */
public record AsignaturaPlanEstudianteProjection(
        UUID idPrograma,
        String nombrePrograma,
        UUID idPlanEstudio,
        Integer inpPlanEstudio,
        UUID idSemestrePlanEstudio,
        Integer numeroSemestre,
        UUID idAsignatura,
        String codigoAsignatura,
        String nombreAsignatura,
        Integer creditos,
        String codigoComponente,
        String codigoCaracter,
        String nombreCaracter,
        boolean cursando,
        UUID idGrupoCursando,
        String nombreGrupoCursando
) {
}
