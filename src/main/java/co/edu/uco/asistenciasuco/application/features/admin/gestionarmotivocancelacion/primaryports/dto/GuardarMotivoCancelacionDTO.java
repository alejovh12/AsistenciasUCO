package co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.primaryports.dto;

import java.util.UUID;

public record GuardarMotivoCancelacionDTO(UUID id, String codigo, String nombre, Integer orden, UUID usuario) {
}
