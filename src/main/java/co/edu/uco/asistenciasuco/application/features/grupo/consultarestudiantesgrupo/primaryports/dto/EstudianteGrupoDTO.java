package co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.dto;

import java.util.UUID;

public record EstudianteGrupoDTO(
        UUID id,
        UUID idEstudiante,
        String documento,
        String nombreCompleto,
        String correo,
        String codigoEstado,
        String nombreEstado
) {
}
