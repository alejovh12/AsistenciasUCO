package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

public record DecanoProjection(
        UUID id,
        UUID idUsuario,
        String numeroIdentificacion,
        String nombreCompleto,
        UUID idFacultad,
        String nombreFacultad,
        boolean estaActivoDecano
) {
}
