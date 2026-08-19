package co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.entity;

import java.util.UUID;

/**
 * Resultado interno del registro de docente desde usuario.
 */
public final class RegistrarDocenteDesdeUsuarioResultadoEntity {

    private final UUID docenteId;
    private final boolean exitoso;
    private final String mensajeUsuario;

    public RegistrarDocenteDesdeUsuarioResultadoEntity(
            final boolean exitoso,
            final String mensajeUsuario
    ) {
        this(null, exitoso, mensajeUsuario);
    }

    public RegistrarDocenteDesdeUsuarioResultadoEntity(
            final UUID docenteId,
            final boolean exitoso,
            final String mensajeUsuario
    ) {
        this.docenteId = docenteId;
        this.exitoso = exitoso;
        this.mensajeUsuario = mensajeUsuario;
    }

    public UUID getDocenteId() {
        return docenteId;
    }

    public boolean isExitoso() {
        return exitoso;
    }

    public String getMensajeUsuario() {
        return mensajeUsuario;
    }

}
