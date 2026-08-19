package co.edu.uco.asistenciasuco.application.secondaryports.repository.entity;

import java.util.UUID;

/**
 * Resultado funcional de operaciones relacionadas con docentes.
 */
public final class DocenteOperacionRepositoryEntity {

    private final UUID docenteId;
    private final String mensajeUsuario;

    public DocenteOperacionRepositoryEntity(final String mensajeUsuario) {
        this(null, mensajeUsuario);
    }

    public DocenteOperacionRepositoryEntity(final UUID docenteId, final String mensajeUsuario) {
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
