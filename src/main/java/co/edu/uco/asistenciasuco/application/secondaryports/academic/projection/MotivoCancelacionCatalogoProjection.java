package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/** Fila de uv_motivo_cancelacion_sesion. */
public record MotivoCancelacionCatalogoProjection(UUID id, String codigo, String nombre, int orden, boolean estaActivo) {
}
