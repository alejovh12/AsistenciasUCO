package co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.usecase.domain;

import java.util.UUID;

public record ActualizarTipoProgramaDomain(
        UUID id,
        String nombre,
        UUID actorUsuarioId
) {
}
