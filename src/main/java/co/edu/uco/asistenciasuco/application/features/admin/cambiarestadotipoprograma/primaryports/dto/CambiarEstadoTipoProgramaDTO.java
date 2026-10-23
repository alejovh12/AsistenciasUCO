package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.primaryports.dto;

import java.util.UUID;

public record CambiarEstadoTipoProgramaDTO(
        UUID id,
        Boolean activo,
        UUID actorUsuarioId
) {
}
