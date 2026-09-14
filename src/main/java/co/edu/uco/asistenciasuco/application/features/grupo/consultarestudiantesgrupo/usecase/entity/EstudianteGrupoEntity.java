package co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.entity;

import java.util.UUID;

public record EstudianteGrupoEntity(
        UUID id,
        UUID idEstudiante,
        String documento,
        String nombreCompleto,
        String correo,
        String codigoEstado,
        String nombreEstado
) {
}
