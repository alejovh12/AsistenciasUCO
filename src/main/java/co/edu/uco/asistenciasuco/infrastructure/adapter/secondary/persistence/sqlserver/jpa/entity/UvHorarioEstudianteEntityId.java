package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Identidad compuesta de {@code dbo.uv_horario_estudiante}: {@code id} es el horario y se repite
 * por cada estudiante del grupo (LB-008 JPA-05, IDENTITY_EVIDENCE).
 */
public final class UvHorarioEstudianteEntityId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID id;
    private UUID idEstudiante;

    public UvHorarioEstudianteEntityId() {
        // requerido por JPA
    }

    public UvHorarioEstudianteEntityId(final UUID id, final UUID idEstudiante) {
        this.id = id;
        this.idEstudiante = idEstudiante;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UvHorarioEstudianteEntityId that)) {
            return false;
        }
        return Objects.equals(id, that.id) && Objects.equals(idEstudiante, that.idEstudiante);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, idEstudiante);
    }
}
