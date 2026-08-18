package co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.dto;

/**
 * DTO de salida publica para asignar docente a grupo.
 */
public final class AsignarDocenteAGrupoResultadoDTO {

    private boolean exitoso;
    private String mensajeUsuario;

    public AsignarDocenteAGrupoResultadoDTO() {
        super();
    }

    public AsignarDocenteAGrupoResultadoDTO(
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
