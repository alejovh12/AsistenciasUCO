package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.primaryports.dto;

import java.util.UUID;

public record CambiarEstadoAreaDTO(
        UUID id,
        Boolean activa,
        UUID actorUsuarioId
) {
}
