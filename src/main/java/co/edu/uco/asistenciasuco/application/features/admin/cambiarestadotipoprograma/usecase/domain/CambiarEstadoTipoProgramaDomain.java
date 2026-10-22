package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.usecase.domain;

import java.util.UUID;

public record CambiarEstadoTipoProgramaDomain(
        UUID id,
        Boolean activo,
        UUID actorUsuarioId
) {
}
