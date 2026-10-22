package co.edu.uco.asistenciasuco.application.secondaryports.academic.command;

import java.util.UUID;

/** Activa o inactiva el vinculo docente-programa (HU093). No afecta a la persona, su rol ni la cuenta de identidad. */
public record CambiarEstadoDocenteProgramaCommand(UUID idDocente, UUID idPrograma, boolean estaActivo, UUID usuarioEjecutor) {
}
