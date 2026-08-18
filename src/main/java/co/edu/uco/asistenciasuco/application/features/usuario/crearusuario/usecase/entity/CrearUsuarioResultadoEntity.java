package co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.entity;

/**
 * Resultado interno de la creacion de usuario.
 */
public final class CrearUsuarioResultadoEntity {

    private final boolean exitoso;
    private final String mensajeUsuario;

    public CrearUsuarioResultadoEntity(
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
