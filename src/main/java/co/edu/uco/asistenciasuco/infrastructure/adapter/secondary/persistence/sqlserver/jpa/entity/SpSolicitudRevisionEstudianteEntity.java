package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;

/**
 * Fila de resultado de {@code dbo.usp_consultar_solicitudes_revision_estudiante} (HU010/HU011). Mismo resultset que
 * la bandeja docente (R2); no es una tabla ni una vista. Los DATETIME2(7) se leen con {@link LocalDateTimeJdbcType}.
 */
@Entity
public class SpSolicitudRevisionEstudianteEntity extends SpSolicitudRevisionDocenteBaseEntity {
    @Column(name = "totalRegistros") private Integer totalRegistros;

    protected SpSolicitudRevisionEstudianteEntity() { }

    public Integer totalRegistros() { return totalRegistros; }
}
