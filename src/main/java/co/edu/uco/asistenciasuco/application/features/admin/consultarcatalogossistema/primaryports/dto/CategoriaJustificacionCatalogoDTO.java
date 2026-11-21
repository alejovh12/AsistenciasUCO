package co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.primaryports.dto;

import java.util.UUID;

/** Categoria de justificacion definida por el sistema (solo lectura). */
public record CategoriaJustificacionCatalogoDTO(UUID id, String codigo, String nombre, String descripcion, boolean requiereSoporte) {
}
