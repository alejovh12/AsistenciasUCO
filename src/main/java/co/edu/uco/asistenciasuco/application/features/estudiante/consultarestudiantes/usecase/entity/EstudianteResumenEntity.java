package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.entity;

import java.util.UUID;

public record EstudianteResumenEntity(
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
