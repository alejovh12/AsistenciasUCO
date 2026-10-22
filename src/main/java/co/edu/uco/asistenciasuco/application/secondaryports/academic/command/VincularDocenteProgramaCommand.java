package co.edu.uco.asistenciasuco.application.secondaryports.academic.command;

import java.util.UUID;

/** Comando de vinculo docente-programa. Exactamente uno de los tres modos viene poblado. {@code passwordHash} nunca es texto plano. */
public record VincularDocenteProgramaCommand(
        UUID idDocente,
        UUID idUsuario,
        UUID idTipoIdentificacion,
        Integer numeroIdentificacion,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String correo,
        String passwordHash,
        UUID idTipoVinculacion,
        UUID idPrograma,
        UUID usuarioEjecutor
) {
}
