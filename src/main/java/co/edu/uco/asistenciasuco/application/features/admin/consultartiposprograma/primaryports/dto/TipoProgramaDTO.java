package co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.dto;

import java.util.UUID;

public record TipoProgramaDTO(UUID id, String codigo, String nombre, boolean activo) {
}
