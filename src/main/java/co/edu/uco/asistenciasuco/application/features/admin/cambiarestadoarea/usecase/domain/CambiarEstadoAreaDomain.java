package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.usecase.domain;

import java.util.UUID;

public record CambiarEstadoAreaDomain(
        UUID id,
        Boolean activa,
        UUID actorUsuarioId
) {
}
