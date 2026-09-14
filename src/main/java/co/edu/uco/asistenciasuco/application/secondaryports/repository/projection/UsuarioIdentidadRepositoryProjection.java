package co.edu.uco.asistenciasuco.application.secondaryports.repository.projection;

import java.util.UUID;

public record UsuarioIdentidadRepositoryProjection(
        UUID id,
        UUID tipoIdentificacionId,
        Integer numeroIdentificacion,
        String primerNombre,
        String primerApellido,
        String correo
) {
}
