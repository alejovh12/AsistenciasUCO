package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fila de resultado de {@code dbo.usp_consultar_solicitudes_revision_docente} (LB-004D, R2). No es una tabla ni
 * una vista. Los DATETIME2(7) se leen con {@link LocalDateTimeJdbcType} (sin zona de la JVM).
 */
@Entity
public class SpSolicitudRevisionDocenteEntity extends SpSolicitudRevisionDocenteBaseEntity {
    @Column(name = "totalRegistros") private Integer totalRegistros;
    // PC (puntos de control): toma reclamada (NULL en un reclamo de la sesion); columnas AL FINAL del resultset (migracion 38)
    @Column(name = "idToma") private UUID idToma;
    @Column(name = "numeroToma") private Integer numeroToma;
    @JdbcType(LocalDateTimeJdbcType.class)
    @Column(name = "fechaPuntoControlUtc") private LocalDateTime fechaPuntoControlUtc;

    protected SpSolicitudRevisionDocenteEntity() { }

    public Integer totalRegistros() { return totalRegistros; }
    public UUID idToma() { return idToma; }
    public Integer numeroToma() { return numeroToma; }
    public LocalDateTime fechaPuntoControlUtc() { return fechaPuntoControlUtc; }
}
