package co.edu.uco.asistenciasuco.application.features.admin.creararea.primaryports.dto;

import java.util.UUID;

public record CrearAreaDTO(
        String codigo,
        String nombre,
        UUID actorUsuarioId
) {
}
