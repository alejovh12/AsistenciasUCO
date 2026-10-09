package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class UvEstudianteEntityId implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private UUID id;
    private UUID idGrupo;
    public UvEstudianteEntityId() { }
    public UvEstudianteEntityId(final UUID id, final UUID idGrupo) { this.id = id; this.idGrupo = idGrupo; }
    @Override public boolean equals(final Object other) {
        if (this == other) return true;
        if (!(other instanceof UvEstudianteEntityId that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(idGrupo, that.idGrupo);
    }
    @Override public int hashCode() { return Objects.hash(id, idGrupo); }
}
