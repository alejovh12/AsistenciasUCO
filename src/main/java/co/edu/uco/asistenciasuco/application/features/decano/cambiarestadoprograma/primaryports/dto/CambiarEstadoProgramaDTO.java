package co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.primaryports.dto;

import java.util.UUID;

public record CambiarEstadoProgramaDTO(
        UUID actorUsuarioId, UUID id, Boolean estaActivo
) {
}
