package co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.primaryports.dto;

import java.util.UUID;

/** IDENT-002: el alta de una persona nueva no lleva contrasena; el docente la define al activar su cuenta. */
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
        UUID idTipoVinculacion
) {
}
