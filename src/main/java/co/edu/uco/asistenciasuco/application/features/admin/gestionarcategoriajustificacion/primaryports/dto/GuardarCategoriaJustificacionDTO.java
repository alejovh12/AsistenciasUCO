package co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.primaryports.dto;

import java.util.UUID;

public record GuardarCategoriaJustificacionDTO(UUID id, String codigo, String nombre, String descripcion, Boolean requiereSoporte, UUID usuario) {
}
