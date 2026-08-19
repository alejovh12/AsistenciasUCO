package co.edu.uco.asistenciasuco.application.secondaryports.repository.entity;

import java.util.UUID;

/**
 * Resultado funcional de creacion de usuario.
 */
public final class CrearUsuarioRepositoryEntity {

    private final UUID usuarioId;
    private final String mensajeUsuario;

    public CrearUsuarioRepositoryEntity(final String mensajeUsuario) {
        this(null, mensajeUsuario);
    }

    public CrearUsuarioRepositoryEntity(final UUID usuarioId, final String mensajeUsuario) {
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
