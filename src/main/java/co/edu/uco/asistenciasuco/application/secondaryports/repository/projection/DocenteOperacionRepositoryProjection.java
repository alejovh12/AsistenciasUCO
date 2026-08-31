package co.edu.uco.asistenciasuco.application.secondaryports.repository.projection;

import java.util.UUID;

/**
 * Resultado funcional de operaciones relacionadas con docentes.
 */
public final class DocenteOperacionRepositoryProjection {

    private final UUID docenteId;
    private final String mensajeUsuario;

    public DocenteOperacionRepositoryProjection(final String mensajeUsuario) {
        this(null, mensajeUsuario);
    }

    public DocenteOperacionRepositoryProjection(final UUID docenteId, final String mensajeUsuario) {
        this.docenteId = docenteId;
        this.mensajeUsuario = mensajeUsuario;
    }

    public UUID getDocenteId() {
        return docenteId;
    }

    public String getMensajeUsuario() {
        return mensajeUsuario;
    }
}
