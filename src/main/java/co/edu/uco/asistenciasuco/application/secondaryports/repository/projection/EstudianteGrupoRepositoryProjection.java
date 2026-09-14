package co.edu.uco.asistenciasuco.application.secondaryports.repository.projection;

import java.util.UUID;

public record EstudianteGrupoRepositoryProjection(
        UUID id,
        UUID idEstudiante,
        String documento,
        String nombreCompleto,
        String correo,
        String codigoEstado,
        String nombreEstado
) {
}
