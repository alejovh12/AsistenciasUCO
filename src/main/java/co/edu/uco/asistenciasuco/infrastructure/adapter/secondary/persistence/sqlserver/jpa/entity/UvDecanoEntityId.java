package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Identidad compuesta de {@code dbo.uv_decano}: un decano puede serlo de varias facultades,
 * por eso {@code id} por sí solo no es único (LB-008 JPA-05, IDENTITY_EVIDENCE).
 */
public final class UvDecanoEntityId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID id;
    private UUID idFacultad;

    public UvDecanoEntityId() {
        // requerido por JPA
    }

    public UvDecanoEntityId(final UUID id, final UUID idFacultad) {
        this.id = id;
        this.idFacultad = idFacultad;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UvDecanoEntityId that)) {
            return false;
        }
        return Objects.equals(id, that.id) && Objects.equals(idFacultad, that.idFacultad);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, idFacultad);
    }
}
