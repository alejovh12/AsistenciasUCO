package co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.entity;

/**
 * Resultado interno del registro de docente desde usuario.
 */
public final class RegistrarDocenteDesdeUsuarioResultadoEntity {

    private final boolean exitoso;
    private final String mensajeUsuario;

    public RegistrarDocenteDesdeUsuarioResultadoEntity(
            final boolean exitoso,
            final String mensajeUsuario
    ) {
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
