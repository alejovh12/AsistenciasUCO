package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

/**
 * DTO del puerto secundario para registrar un docente desde un usuario existente.
 */
public final class RegistrarDocenteDesdeUsuarioRepositoryDTO {

    private UUID usuario;

    public RegistrarDocenteDesdeUsuarioRepositoryDTO() {
        super();
    }

    public RegistrarDocenteDesdeUsuarioRepositoryDTO(final UUID usuario) {
        setUsuario(usuario);
    }

    public UUID getUsuario() {
        return usuario;
    }

    public void setUsuario(final UUID usuario) {
        this.usuario = usuario;
    }

}
