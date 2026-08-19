package co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.entity;

import java.util.UUID;

/**
 * Resultado interno de la creacion de usuario.
 */
public final class CrearUsuarioResultadoEntity {

    private final UUID usuarioId;
    private final boolean exitoso;
    private final String mensajeUsuario;

    public CrearUsuarioResultadoEntity(
            final boolean exitoso,
            final String mensajeUsuario
    ) {
        this(null, exitoso, mensajeUsuario);
    }

    public CrearUsuarioResultadoEntity(
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
