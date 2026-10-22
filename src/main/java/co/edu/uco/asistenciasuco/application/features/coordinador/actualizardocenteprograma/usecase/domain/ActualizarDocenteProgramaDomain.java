package co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.usecase.domain;

import java.util.UUID;

public record ActualizarDocenteProgramaDomain(
        UUID actorUsuarioId,
        UUID idDocente,
        UUID idTipoVinculacion,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido
) {
}
