package co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.primaryports.dto;

import java.util.UUID;

public record ActualizarTipoProgramaDTO(
        UUID id,
        String nombre,
        UUID actorUsuarioId
) {
}
