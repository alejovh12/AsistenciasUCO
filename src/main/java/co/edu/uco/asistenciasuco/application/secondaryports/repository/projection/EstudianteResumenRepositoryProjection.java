package co.edu.uco.asistenciasuco.application.secondaryports.repository.projection;

import java.util.UUID;

public record EstudianteResumenRepositoryProjection(
        UUID id,
        UUID idUsuario,
        UUID tipoIdentificacionId,
        Integer numeroIdentificacion,
        String primerApellido,
        String segundoApellido,
        String primerNombre,
        String segundoNombre,
        String nombreCompleto,
        String correo,
        boolean estaActivoUsuario
) {
}
