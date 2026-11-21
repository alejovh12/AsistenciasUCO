package co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.usecase.domain;

import java.util.UUID;

/** Datos de entrada para crear o actualizar un registro del catalogo de categorias de justificacion. */
public record CategoriaJustificacionDomain(UUID id, String codigo, String nombre, String descripcion, Boolean requiereSoporte, UUID usuario) {
}
