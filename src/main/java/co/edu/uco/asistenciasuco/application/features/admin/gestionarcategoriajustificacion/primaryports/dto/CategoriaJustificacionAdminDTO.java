package co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.primaryports.dto;

import java.util.UUID;

public record CategoriaJustificacionAdminDTO(UUID id, String codigo, String nombre, String descripcion, boolean requiereSoporte, boolean estaActivo) {
}
