package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.Instant;
import java.util.UUID;

public record DocenteProgramaProjection(
        UUID idDocentePrograma,
        UUID idDocente,
        UUID idUsuario,
        String codigoTipoIdentificacion,
        Integer numeroIdentificacion,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String nombreCompleto,
        String correo,
        UUID idTipoVinculacion,
        String codigoTipoVinculacion,
        String nombreTipoVinculacion,
        Instant fechaVinculacionUtc,
        int totalGruposPrograma,
        boolean estaActivo
) {
}
