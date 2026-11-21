package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/** Fila de uv_categoria_justificacion. */
public record CategoriaJustificacionCatalogoProjection(UUID id, String codigo, String nombre, String descripcion, boolean requiereSoporte) {
}
