package co.edu.uco.asistenciasuco.application.secondaryports.academic.command;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO;

import java.util.UUID;

/**
 * Edicion de un vinculo docente-programa (HU092; sin tipo de vinculacion desde MEJORAS-SERGIO-2026-11-18). Los campos nulos no se modifican; "" en segundo nombre/apellido los
 * limpia. HU159: {@code documento} corrige el tipo y el numero de documento (opcional, con motivo).
 */
public record ActualizarDocenteProgramaCommand(
        UUID idDocente,
        UUID idPrograma,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        UUID usuarioEjecutor,
        CorreccionDocumentoDTO documento
) {

    /** Edicion sin correccion de documento (contrato original de HU092). */
    public ActualizarDocenteProgramaCommand(final UUID idDocente, final UUID idPrograma,
            final String primerNombre, final String segundoNombre, final String primerApellido,
            final String segundoApellido, final UUID usuarioEjecutor) {
        this(idDocente, idPrograma, primerNombre, segundoNombre, primerApellido, segundoApellido,
                usuarioEjecutor, CorreccionDocumentoDTO.ninguna());
    }
}
