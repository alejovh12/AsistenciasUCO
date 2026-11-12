package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/**
 * PC (puntos de control): minutos de la sesion del estudiante autenticado por clasificacion de tramo y su estado en la
 * sesion ({@code uv_asistencia_sesion_horas}). {@code codigoEstadoSesion}: PRESENTE | AUSENTE | JUSTIFICADA | PARCIAL |
 * NO_DISPONIBLE. Una sesion sin cabecera de asistencia no genera fila.
 */
public record MinutosSesionEstudianteProjection(
        UUID idSesion,
        int minutosSesion,
        int minutosAsistidos,
        int minutosFalta,
        int minutosExcusa,
        int minutosSinRegistro,
        String codigoEstadoSesion
) {
}
