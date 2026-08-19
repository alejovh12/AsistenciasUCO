package co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.dto;

import java.util.UUID;

/**
 * DTO de salida funcional para crear usuario.
 */
public final class CrearUsuarioResultadoDTO {

    private UUID usuarioId;
    private boolean exitoso;
    private String mensajeUsuario;

    public CrearUsuarioResultadoDTO() {
        super();
    }

    public CrearUsuarioResultadoDTO(
            final boolean exitoso,
            final String mensajeUsuario
    ) {
        this(null, exitoso, mensajeUsuario);
    }

    public CrearUsuarioResultadoDTO(
            final UUID usuarioId,
            final boolean exitoso,
            final String mensajeUsuario
    ) {
        setUsuarioId(usuarioId);
        setExitoso(exitoso);
        setMensajeUsuario(mensajeUsuario);
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(final UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    public boolean isExitoso() {
        return exitoso;
    }

    public void setExitoso(final boolean exitoso) {
        this.exitoso = exitoso;
    }

    public String getMensajeUsuario() {
        return mensajeUsuario;
    }

    public void setMensajeUsuario(final String mensajeUsuario) {
        this.mensajeUsuario = mensajeUsuario;
    }

}
