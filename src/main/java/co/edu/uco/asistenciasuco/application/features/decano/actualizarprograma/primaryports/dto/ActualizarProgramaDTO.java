package co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.primaryports.dto;

import java.util.UUID;

public record ActualizarProgramaDTO(
        UUID actorUsuarioId, UUID id, String nombre, UUID idTipoPrograma, UUID idCoordinador, UUID idModalidad, UUID idJornada
) {
}
