package co.edu.uco.asistenciasuco.application.secondaryports.academic.command;

import java.util.UUID;

/** Datos para crear o actualizar un registro del catalogo de categorias de justificacion (migracion 65 de la BD). */
public record GuardarCategoriaJustificacionCommand(UUID id, String codigo, String nombre, String descripcion, boolean requiereSoporte, UUID usuario) {
}
