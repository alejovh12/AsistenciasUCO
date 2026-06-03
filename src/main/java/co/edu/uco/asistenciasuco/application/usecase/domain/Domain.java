package co.edu.uco.asistenciasuco.application.usecase.domain;

import java.util.UUID;

/**
 * Clase base global para entidades/agregados de dominio.
 */
public abstract class Domain {

    private UUID id;

    protected Domain(final UUID id) {
        setId(id);
    }

    public UUID getId() {
        return id;
    }

    protected void setId(final UUID id) {
        this.id = id;
    }

    protected UUID generateId() {
        return UUID.randomUUID();
    }
}
