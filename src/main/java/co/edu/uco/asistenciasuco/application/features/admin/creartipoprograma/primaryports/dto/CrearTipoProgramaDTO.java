package co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.primaryports.dto;

import java.util.UUID;

public record CrearTipoProgramaDTO(
        String codigo,
        String nombre,
        UUID actorUsuarioId
) {
}
