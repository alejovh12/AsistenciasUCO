package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaPlanEstudianteProjection;

import java.util.List;
import java.util.UUID;

/**
 * HU018: estructura curricular por semestres de los planes activos de los programas del estudiante autenticado. Recibe el
 * USUARIO ejecutor; el SP resuelve el estudiante y solo devuelve los planes de sus propios programas, ya ordenados por
 * programa, plan, semestre, caracter y nombre de la asignatura.
 */
public interface PlanEstudioEstudianteQueryPort {

    List<AsignaturaPlanEstudianteProjection> consultarPlanEstudio(UUID usuarioEjecutor);
}
