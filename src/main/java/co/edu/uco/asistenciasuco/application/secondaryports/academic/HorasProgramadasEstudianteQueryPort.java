package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorasProgramadasEstudianteProjection;

import java.util.List;
import java.util.UUID;

/**
 * HU019: horas programadas de las materias del estudiante autenticado. Recibe el USUARIO ejecutor; el SP resuelve el
 * estudiante y solo devuelve sus matriculas inscritas (el mismo universo de {@link MateriaEstudianteQueryPort}).
 */
public interface HorasProgramadasEstudianteQueryPort {

    List<HorasProgramadasEstudianteProjection> consultarHorasProgramadas(UUID usuarioEjecutor);
}
