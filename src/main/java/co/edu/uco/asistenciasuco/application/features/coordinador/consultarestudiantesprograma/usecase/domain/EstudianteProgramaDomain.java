package co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.domain;

import java.util.UUID;

/**
 * Modelo de resultado interno de la consulta de estudiantes de un programa.
 */
public record EstudianteProgramaDomain(
        UUID id,
        UUID idUsuario,
        String numeroIdentificacion,
        String nombreCompleto,
        String correo,
        UUID idPrograma,
        String nombrePrograma
) {
}
