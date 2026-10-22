package co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.domain;

import java.util.UUID;

public record VincularDocenteProgramaDomain(
        UUID actorUsuarioId,
        UUID idDocente,
        UUID idUsuario,
        UUID idTipoIdentificacion,
        Integer numeroIdentificacion,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String correo,
        String password,
        UUID idTipoVinculacion
) {
}
