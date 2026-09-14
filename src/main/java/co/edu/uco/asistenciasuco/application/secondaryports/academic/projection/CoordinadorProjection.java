package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

public record CoordinadorProjection(
        UUID id,
        UUID idUsuario,
        String numeroIdentificacion,
        String nombreCompleto,
        UUID idPrograma,
        String nombrePrograma,
        boolean estaActivoCoordinador
) {
}
