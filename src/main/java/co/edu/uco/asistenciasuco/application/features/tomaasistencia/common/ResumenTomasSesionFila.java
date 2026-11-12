package co.edu.uco.asistenciasuco.application.features.tomaasistencia.common;

import java.util.UUID;

/**
 * PC (puntos de control): resumen de un estudiante en una sesion tal como lo entrega
 * {@code usp_consultar_resumen_tomas_sesion}: minutos por clasificacion de tramo y estado de la sesion para el estudiante
 * ({@code PRESENTE}, {@code AUSENTE}, {@code JUSTIFICADA}, {@code PARCIAL} o {@code NO_DISPONIBLE}). Java no recalcula.
 */
public record ResumenTomasSesionFila(
        UUID idEstudiante,
        String nombreCompleto,
        int minutosSesion,
        int minutosAsistidos,
        int minutosFalta,
        int minutosExcusa,
        int minutosSinRegistro,
        String estado,
        int tomasAusentes
) {
}
