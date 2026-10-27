package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SolicitudRevisionEstudianteProjection;

import java.util.List;
import java.util.UUID;

/**
 * Consulta de las solicitudes de revision del estudiante autenticado (HU010/HU011). El ownership se resuelve en
 * SQL por {@code Estudiante.usuario = usuarioEjecutor}: cualquier otro usuario obtiene cero filas.
 */
public interface SolicitudRevisionEstudianteQueryPort {

    List<SolicitudRevisionEstudianteProjection> consultarSolicitudesEstudiante(
            UUID usuarioEjecutor,
            String codigoEstado,
            int numeroPagina,
            int tamanoPagina
    );
}
