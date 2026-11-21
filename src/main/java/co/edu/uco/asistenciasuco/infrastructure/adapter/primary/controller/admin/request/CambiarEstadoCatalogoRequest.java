package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request;

import jakarta.validation.constraints.NotNull;

/** Cuerpo de {@code PATCH .../{id}/estado} de los catalogos administrados: el estado deseado (idempotente). */
public record CambiarEstadoCatalogoRequest(@NotNull(message = "El campo estaActivo es obligatorio.") Boolean estaActivo) {
}
