package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/** motivos de cancelacion de sesion: fila de la vista de administracion (activos e inactivos). */
public record MotivoCancelacionAdminProjection(UUID id, String codigo, String nombre, int orden, boolean estaActivo) {
}
