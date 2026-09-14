package co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.entity;

import java.util.UUID;

/**
 * Resultado interno de provisionar usuario.
 */
public final class ProvisionarUsuarioResultadoEntity {

    private final UUID usuarioId;
    private final boolean exitoso;
    private final String mensajeUsuario;

    public ProvisionarUsuarioResultadoEntity(
            final UUID usuarioId,
            final boolean exitoso,
            final String mensajeUsuario
    ) {
        this.usuarioId = usuarioId;
        this.exitoso = exitoso;
        this.mensajeUsuario = mensajeUsuario;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public boolean isExitoso() {
        return exitoso;
    }

    public String getMensajeUsuario() {
        return mensajeUsuario;
    }
}
