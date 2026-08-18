package co.edu.uco.asistenciasuco.application.secondaryports.security;

import java.util.UUID;

public final class UsuarioAutenticacionData {

    private final UUID idUsuario;
    private final String correo;
    private final String passwordHash;
    private final boolean correoConfirmado;
    private final boolean activo;

    public UsuarioAutenticacionData(
            final UUID idUsuario,
            final String correo,
            final String passwordHash,
            final boolean correoConfirmado,
            final boolean activo
    ) {
        this.idUsuario = idUsuario;
        this.correo = correo;
        this.passwordHash = passwordHash;
        this.correoConfirmado = correoConfirmado;
        this.activo = activo;
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public String getCorreo() {
        return correo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isCorreoConfirmado() {
        return correoConfirmado;
    }

    public boolean isActivo() {
        return activo;
    }
}
