package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Identidad compuesta de {@code dbo.uv_coordinador}: un coordinador puede coordinar varios programas,
 * por eso {@code id} por sí solo no es único (LB-008 JPA-05, IDENTITY_EVIDENCE).
 */
public final class UvCoordinadorEntityId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID id;
    private UUID idPrograma;

    public UvCoordinadorEntityId() {
        // requerido por JPA
    }

    public UvCoordinadorEntityId(final UUID id, final UUID idPrograma) {
        this.id = id;
        this.idPrograma = idPrograma;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UvCoordinadorEntityId that)) {
            return false;
        }
        return Objects.equals(id, that.id) && Objects.equals(idPrograma, that.idPrograma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, idPrograma);
    }
}
