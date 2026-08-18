package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.entity;

/**
 * Resultado interno del registro de estudiante en grupo.
 */
public final class RegistrarEstudianteResultadoEntity {

    private final boolean exitoso;
    private final String mensajeUsuario;

    public RegistrarEstudianteResultadoEntity(final boolean exitoso, final String mensajeUsuario) {
        this.exitoso = exitoso;
        this.mensajeUsuario = mensajeUsuario;
    }

    public boolean isExitoso() {
        return exitoso;
    }

    public String getMensajeUsuario() {
        return mensajeUsuario;
    }
}
