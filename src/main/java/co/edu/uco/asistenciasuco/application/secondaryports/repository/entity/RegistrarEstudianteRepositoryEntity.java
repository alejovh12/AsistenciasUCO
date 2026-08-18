package co.edu.uco.asistenciasuco.application.secondaryports.repository.entity;

/**
 * Resultado funcional del registro de estudiante en grupo.
 */
public final class RegistrarEstudianteRepositoryEntity {

    private final String mensajeUsuario;

    public RegistrarEstudianteRepositoryEntity(final String mensajeUsuario) {
        this.mensajeUsuario = mensajeUsuario;
    }

    public String getMensajeUsuario() {
        return mensajeUsuario;
    }
}
