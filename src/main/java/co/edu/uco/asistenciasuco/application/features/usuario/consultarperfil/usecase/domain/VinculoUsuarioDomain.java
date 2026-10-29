package co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.usecase.domain;

import java.util.UUID;

public record VinculoUsuarioDomain(
        String rol,
        UUID idEntidad,
        String nombreEntidad,
        String detalle,
        boolean estaActivo
) {
}
