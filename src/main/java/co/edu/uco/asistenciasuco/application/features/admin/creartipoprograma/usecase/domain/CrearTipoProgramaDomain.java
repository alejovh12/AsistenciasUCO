package co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.usecase.domain;

import java.util.UUID;

public record CrearTipoProgramaDomain(
        String codigo,
        String nombre,
        UUID actorUsuarioId
) {
}
