package co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.primaryports.dto;

import java.util.UUID;

public record VinculoUsuarioDTO(
        String rol,
        UUID idEntidad,
        String nombreEntidad,
        String detalle,
        boolean estaActivo
) {
}
