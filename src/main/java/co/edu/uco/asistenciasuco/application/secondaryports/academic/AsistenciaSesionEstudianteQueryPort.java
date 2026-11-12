package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsistenciaSesionEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsistenciaTomaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MinutosSesionEstudianteProjection;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Lectura de la asistencia propia del estudiante en las sesiones de una asignatura.
 * El estudiante lo resuelve el backend a partir del usuario autenticado; jamas llega desde el cliente.
 */
public interface AsistenciaSesionEstudianteQueryPort {

    List<AsistenciaSesionEstudianteProjection> consultarAsistenciaSesiones(UUID idEstudiante, UUID idAsignatura);

    /**
     * Misma lectura acotada a las sesiones indicadas (las de la pagina que se esta devolviendo). El estudiante y la
     * asignatura siguen siendo parte del filtro: un id de sesion ajeno no devuelve filas.
     */
    List<AsistenciaSesionEstudianteProjection> consultarAsistenciaSesiones(UUID idEstudiante, UUID idAsignatura,
                                                                           Collection<UUID> idsSesion);

    /**
     * PC (puntos de control): una fila por (sesion, toma) con el estado del estudiante en esa toma y si es reclamable. Mismo
     * aislamiento por estudiante y asignatura; una sesion sin cabecera de asistencia no devuelve filas.
     */
    List<AsistenciaTomaEstudianteProjection> consultarTomasSesiones(UUID idEstudiante, UUID idAsignatura,
                                                                    Collection<UUID> idsSesion);

    /** PC (puntos de control): minutos y estado de cada sesion (una fila por sesion con cabecera de asistencia). */
    List<MinutosSesionEstudianteProjection> consultarMinutosSesiones(UUID idEstudiante, UUID idAsignatura,
                                                                     Collection<UUID> idsSesion);
}
