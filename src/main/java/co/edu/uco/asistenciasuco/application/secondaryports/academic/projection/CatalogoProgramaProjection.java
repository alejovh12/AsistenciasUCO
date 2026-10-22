package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/** Item de catalogo de programa (tipo de programa, modalidad o jornada). */
public record CatalogoProgramaProjection(UUID id, String codigo, String nombre) {
}
