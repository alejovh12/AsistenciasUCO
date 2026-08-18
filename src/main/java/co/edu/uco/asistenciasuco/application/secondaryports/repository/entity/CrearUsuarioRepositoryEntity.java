package co.edu.uco.asistenciasuco.application.secondaryports.repository.entity;

/**
 * Resultado funcional de creacion de usuario.
 */
public final class CrearUsuarioRepositoryEntity {

    private final String mensajeUsuario;

    public CrearUsuarioRepositoryEntity(final String mensajeUsuario) {
        this.mensajeUsuario = mensajeUsuario;
    }

    public String getMensajeUsuario() {
        return mensajeUsuario;
    }
}
