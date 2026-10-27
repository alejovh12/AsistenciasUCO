package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DestinatarioSolicitudIngresoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SolicitudIngresoDocenteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SolicitudIngresoEstudianteProjection;

import java.util.List;
import java.util.UUID;

/**
 * HU053/HU054/HU055: solicitudes de ingreso a un grupo. El provider DB valida el ambito (estudiante propietario o
 * docente titular), el periodo, la matricula y el cupo con bloqueo; las solicitudes ajenas son indistinguibles de las
 * inexistentes. Los codigos de estado son los de dbo.Estado: P, A, R, E (en espera) y X (retirada).
 */
public interface SolicitudIngresoGrupoPort {

    void registrarSolicitud(UUID idSolicitud, UUID idGrupo, UUID idUsuarioEjecutor);

    /** @return codigo del estado resultante: A (matriculado), E (sin cupo, en espera) o R (rechazada). */
    String resolverSolicitud(UUID idSolicitud, String accion, String motivo, UUID idUsuarioEjecutor);

    void retirarSolicitud(UUID idSolicitud, UUID idUsuarioEjecutor);

    List<SolicitudIngresoDocenteProjection> consultarSolicitudesGrupo(
            UUID idGrupo, String codigoEstado, int numeroPagina, int tamanoPagina, UUID idUsuarioEjecutor);

    List<SolicitudIngresoEstudianteProjection> consultarSolicitudesEstudiante(
            String codigoEstado, int numeroPagina, int tamanoPagina, UUID idUsuarioEjecutor);

    /** Estudiante solicitante y docente titular; vacio si el ejecutor no es ninguno de los dos. */
    List<DestinatarioSolicitudIngresoProjection> consultarDestinatarios(UUID idSolicitud, UUID idUsuarioEjecutor);
}
