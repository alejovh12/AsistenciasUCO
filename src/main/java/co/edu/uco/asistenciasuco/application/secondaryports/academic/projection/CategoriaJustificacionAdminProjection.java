package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/** categorias de justificacion: fila de la vista de administracion (activos e inactivos). */
public record CategoriaJustificacionAdminProjection(UUID id, String codigo, String nombre, String descripcion, boolean requiereSoporte, boolean estaActivo) {
}
