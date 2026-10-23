package co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.usecase.domain;

import java.util.UUID;

public record CambiarEstadoProgramaDomain(
        UUID actorUsuarioId, UUID id, Boolean estaActivo
) {
}
