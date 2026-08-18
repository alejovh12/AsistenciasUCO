package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto;

import java.util.UUID;

public record EstudianteResumenDTO(
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
