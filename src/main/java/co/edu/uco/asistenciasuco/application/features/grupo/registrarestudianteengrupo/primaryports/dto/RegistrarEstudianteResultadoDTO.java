package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto;

/**
 * DTO de salida publica para registrar estudiante en grupo.
 */
public final class RegistrarEstudianteResultadoDTO {

    private boolean exitoso;
    private String mensajeUsuario;

    public RegistrarEstudianteResultadoDTO() {
        super();
    }

    public RegistrarEstudianteResultadoDTO(final boolean exitoso, final String mensajeUsuario) {
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
