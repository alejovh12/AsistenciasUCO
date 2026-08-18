package co.edu.uco.asistenciasuco.application.secondaryports.repository.entity;

/**
 * Resultado funcional de operaciones relacionadas con docentes.
 */
public final class DocenteOperacionRepositoryEntity {

    private final String mensajeUsuario;

    public DocenteOperacionRepositoryEntity(final String mensajeUsuario) {
        this.mensajeUsuario = mensajeUsuario;
    }

    public String getMensajeUsuario() {
        return mensajeUsuario;
    }
}
