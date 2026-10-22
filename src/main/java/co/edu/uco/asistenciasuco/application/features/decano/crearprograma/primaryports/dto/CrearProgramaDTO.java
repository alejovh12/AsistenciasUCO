package co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.dto;

import java.util.UUID;

public record CrearProgramaDTO(
        UUID actorUsuarioId, String nombre, UUID idTipoPrograma, UUID idCoordinador, UUID idModalidad, UUID idJornada
) {
}
