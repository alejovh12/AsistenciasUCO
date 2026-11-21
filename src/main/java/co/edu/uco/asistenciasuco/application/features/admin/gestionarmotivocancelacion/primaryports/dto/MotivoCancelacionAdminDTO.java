package co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.primaryports.dto;

import java.util.UUID;

public record MotivoCancelacionAdminDTO(UUID id, String codigo, String nombre, int orden, boolean estaActivo) {
}
