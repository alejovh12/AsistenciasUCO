package co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.dto;

import java.util.UUID;

/**
 * DTO de salida funcional para registrar un docente desde usuario.
 */
public final class RegistrarDocenteDesdeUsuarioResultadoDTO {

    private UUID docenteId;
    private boolean exitoso;
    private String mensajeUsuario;

    public RegistrarDocenteDesdeUsuarioResultadoDTO() {
        super();
    }

    public RegistrarDocenteDesdeUsuarioResultadoDTO(
            final boolean exitoso,
            final String mensajeUsuario
    ) {
        this(null, exitoso, mensajeUsuario);
    }

    public RegistrarDocenteDesdeUsuarioResultadoDTO(
            final UUID docenteId,
            final boolean exitoso,
            final String mensajeUsuario
    ) {
        setDocenteId(docenteId);
        setExitoso(exitoso);
        setMensajeUsuario(mensajeUsuario);
    }

    public UUID getDocenteId() {
        return docenteId;
    }

    public void setDocenteId(final UUID docenteId) {
        this.docenteId = docenteId;
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
