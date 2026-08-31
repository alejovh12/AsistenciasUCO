package co.edu.uco.asistenciasuco.application.secondaryports.repository.projection;

import java.util.UUID;

/**
 * Resultado funcional de creacion de usuario.
 */
public final class CrearUsuarioRepositoryProjection {

    private final UUID usuarioId;
    private final String mensajeUsuario;

    public CrearUsuarioRepositoryProjection(final String mensajeUsuario) {
        this(null, mensajeUsuario);
    }

    public CrearUsuarioRepositoryProjection(final UUID usuarioId, final String mensajeUsuario) {
        this.usuarioId = usuarioId;
        this.mensajeUsuario = mensajeUsuario;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getMensajeUsuario() {
        return mensajeUsuario;
    }
}
