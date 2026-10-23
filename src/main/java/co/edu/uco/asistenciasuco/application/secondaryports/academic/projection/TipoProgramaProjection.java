package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

public record TipoProgramaProjection(UUID id, String codigo, String nombre, boolean estaActivo) {
}
