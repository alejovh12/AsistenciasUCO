package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.dto;

import java.util.UUID;

public record CrearCoordinadorDTO(
        String numeroIdentificacion,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String correo,
        UUID idPrograma,
        String password,
        UUID usuario
) {
}
