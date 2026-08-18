package co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.dto;

import java.util.UUID;

/**
 * DTO de entrada para registrar un docente desde un usuario existente.
 */
public final class RegistrarDocenteDesdeUsuarioDTO {

    private UUID usuario;

    public RegistrarDocenteDesdeUsuarioDTO() {
        super();
    }

    public RegistrarDocenteDesdeUsuarioDTO(final UUID usuario) {
        setUsuario(usuario);
    }

    public UUID getUsuario() {
        return usuario;
    }

    public void setUsuario(final UUID usuario) {
        this.usuario = usuario;
    }

}
