package co.edu.uco.asistenciasuco.application.secondaryports.repository.projection;

/**
 * Resultado funcional del registro de estudiante en grupo.
 */
public final class RegistrarEstudianteRepositoryProjection {

    private final String mensajeUsuario;

    public RegistrarEstudianteRepositoryProjection(final String mensajeUsuario) {
        this.mensajeUsuario = mensajeUsuario;
    }

    public String getMensajeUsuario() {
        return mensajeUsuario;
    }
}
