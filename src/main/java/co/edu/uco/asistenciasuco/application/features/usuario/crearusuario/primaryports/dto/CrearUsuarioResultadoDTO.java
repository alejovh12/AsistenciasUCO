package co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.dto;

/**
 * DTO de salida publica para crear usuario.
 */
public final class CrearUsuarioResultadoDTO {

    private boolean exitoso;
    private String mensajeUsuario;

    public CrearUsuarioResultadoDTO() {
        super();
    }

    public CrearUsuarioResultadoDTO(
            final boolean exitoso,
            final String mensajeUsuario
    ) {
        setExitoso(exitoso);
        setMensajeUsuario(mensajeUsuario);
    }

    public boolean isExitoso() {
        return exitoso;
    }

    public void setExitoso(final boolean exitoso) {
        this.exitoso = exitoso;
    }

    public String getMensajeUsuario() {
        return mensajeUsuario;
    }

    public void setMensajeUsuario(final String mensajeUsuario) {
        this.mensajeUsuario = mensajeUsuario;
    }

}
