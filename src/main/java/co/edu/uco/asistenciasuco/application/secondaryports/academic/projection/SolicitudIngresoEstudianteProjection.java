package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.Instant;
import java.util.UUID;

/** Solicitud de ingreso propia del estudiante autenticado (HU053/HU055). */
public record SolicitudIngresoEstudianteProjection(
        UUID idSolicitud,
        UUID idGrupo,
        Integer codigoGrupo,
        String nombreGrupo,
        UUID idAsignatura,
        String nombreAsignatura,
        String nombreDocente,
        String codigoEstado,
        String nombreEstado,
        Instant fechaSolicitudUtc,
        Instant fechaResolucionUtc,
        String motivoRechazo,
        int totalRegistros
) {
}
