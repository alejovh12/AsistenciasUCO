package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.Instant;
import java.util.UUID;

/** Fila de la bandeja de solicitudes de ingreso de un grupo para su docente titular (HU053-HU055). */
public record SolicitudIngresoDocenteProjection(
        UUID idSolicitud,
        UUID idEstudiante,
        String nombreEstudiante,
        Integer numeroIdentificacion,
        String codigoEstado,
        String nombreEstado,
        Instant fechaSolicitudUtc,
        Instant fechaResolucionUtc,
        String motivoRechazo,
        Integer posicionEspera,
        int capacidadGrupo,
        int cuposDisponibles,
        int totalRegistros
) {
}
