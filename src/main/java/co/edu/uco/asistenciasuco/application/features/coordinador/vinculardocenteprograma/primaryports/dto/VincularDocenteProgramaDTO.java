package co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.primaryports.dto;

import java.util.UUID;

/** {@code password} viaja en claro solo dentro de la aplicacion: el caso de uso lo codifica antes de persistir. */
public record VincularDocenteProgramaDTO(
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
