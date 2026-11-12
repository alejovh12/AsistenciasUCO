package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.Instant;
import java.util.UUID;

/**
 * PC (puntos de control): hecho de una toma de una sesion del estudiante autenticado ({@code uv_asistencia_toma_estudiante}).
 * {@code codigoEstadoToma}: PRESENTE | AUSENTE | JUSTIFICADA | SIN_REGISTRO. {@code esReclamable}, {@code idSolicitud},
 * {@code codigoEstadoSolicitud} (P, A, R) y {@code codigoAprobacion} (AN o EX) los calcula la base de datos.
 */
public record AsistenciaTomaEstudianteProjection(
        UUID idSesion,
        UUID idToma,
        Integer numeroToma,
        Integer ordenPuntoControl,
        Instant fechaPuntoControlUtc,
        String codigoEstadoToma,
        boolean esReclamable,
        UUID idSolicitud,
        String codigoEstadoSolicitud,
        String codigoAprobacion
) {
}
