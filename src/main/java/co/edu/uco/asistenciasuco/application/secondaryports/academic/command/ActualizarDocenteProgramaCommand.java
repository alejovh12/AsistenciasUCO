package co.edu.uco.asistenciasuco.application.secondaryports.academic.command;

import java.util.UUID;

/** Edicion de un vinculo docente-programa (HU092). Los campos nulos no se modifican; "" en segundo nombre/apellido los limpia. */
public record ActualizarDocenteProgramaCommand(
        UUID idDocente,
        UUID idPrograma,
        UUID idTipoVinculacion,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        UUID usuarioEjecutor
) {
}
