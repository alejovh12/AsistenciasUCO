package co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.primaryports.dto;

import java.util.UUID;

public record ActualizarDocenteProgramaDTO(
        UUID actorUsuarioId,
        UUID idDocente,
        UUID idTipoVinculacion,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido
) {
}
