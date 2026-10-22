package co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.usecase.domain;

import java.util.UUID;

public record ActualizarProgramaDomain(
        UUID actorUsuarioId, UUID id, String nombre, UUID idTipoPrograma, UUID idCoordinador, UUID idModalidad, UUID idJornada
) {
}
