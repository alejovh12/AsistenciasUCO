package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/** Datos minimos (D4): sin correo. */
public record DocenteCandidatoProjection(
        UUID idDocente,
        UUID idUsuario,
        String codigoTipoIdentificacion,
        Integer numeroIdentificacion,
        String nombreCompleto
) {
}
