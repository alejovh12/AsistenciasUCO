package co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.domain;

import java.util.UUID;

public record CrearProgramaDomain(
        UUID actorUsuarioId, String nombre, UUID idTipoPrograma, UUID idCoordinador, UUID idModalidad, UUID idJornada
) {
}
