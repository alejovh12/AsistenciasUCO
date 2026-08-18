package co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.dto;

/**
 * DTO de salida publica para registrar un docente desde usuario.
 */
public final class RegistrarDocenteDesdeUsuarioResultadoDTO {

    private boolean exitoso;
    private String mensajeUsuario;

    public RegistrarDocenteDesdeUsuarioResultadoDTO() {
        super();
    }

    public RegistrarDocenteDesdeUsuarioResultadoDTO(
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
