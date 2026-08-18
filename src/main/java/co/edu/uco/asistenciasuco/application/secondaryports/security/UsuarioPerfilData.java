package co.edu.uco.asistenciasuco.application.secondaryports.security;

import java.util.UUID;

public final class UsuarioPerfilData {

    private final UUID idUsuario;
    private final UUID idPerfil;
    private final String codigoPerfil;
    private final String nombrePerfil;
    private final String estado;

    public UsuarioPerfilData(
            final UUID idUsuario,
            final UUID idPerfil,
            final String codigoPerfil,
            final String nombrePerfil,
            final String estado
    ) {
        this.idUsuario = idUsuario;
        this.idPerfil = idPerfil;
        this.codigoPerfil = codigoPerfil;
        this.nombrePerfil = nombrePerfil;
        this.estado = estado;
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public UUID getIdPerfil() {
        return idPerfil;
    }

    public String getCodigoPerfil() {
        return codigoPerfil;
    }

    public String getNombrePerfil() {
        return nombrePerfil;
    }

    public String getEstado() {
        return estado;
    }
}
