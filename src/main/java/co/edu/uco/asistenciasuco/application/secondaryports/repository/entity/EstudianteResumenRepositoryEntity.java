package co.edu.uco.asistenciasuco.application.secondaryports.repository.entity;

import java.util.UUID;

public record EstudianteResumenRepositoryEntity(
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
