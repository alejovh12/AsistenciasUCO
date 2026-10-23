package co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.primaryports.dto;

import java.util.UUID;

public record ActualizarCoordinadorDTO(
        UUID idCoordinador,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String correo,
        UUID usuario
) {
}
