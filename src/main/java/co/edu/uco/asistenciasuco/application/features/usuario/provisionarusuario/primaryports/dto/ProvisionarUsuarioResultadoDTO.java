package co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.dto;

import java.util.UUID;

/**
 * DTO de salida funcional de provisionar usuario.
 */
public record ProvisionarUsuarioResultadoDTO(
        UUID usuarioId,
        boolean exitoso,
        String mensajeUsuario
) {
}
