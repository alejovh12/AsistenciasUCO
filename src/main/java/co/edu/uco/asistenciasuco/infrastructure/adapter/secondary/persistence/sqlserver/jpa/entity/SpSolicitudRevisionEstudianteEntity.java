package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fila de resultado de {@code dbo.usp_consultar_solicitudes_revision_estudiante} (HU010/HU011). Mismo resultset que
 * la bandeja docente (R2); no es una tabla ni una vista. Los DATETIME2(7) se leen con {@link LocalDateTimeJdbcType}.
 */
@Entity
public class SpSolicitudRevisionEstudianteEntity extends SpSolicitudRevisionDocenteBaseEntity {
    @Column(name = "totalRegistros") private Integer totalRegistros;
    // CULM-001 D.2: arreglo JSON [{idArchivo, nombreOriginal, tipoContenido, tamanioBytes}] ordenado por adjuncion
    @Column(name = "soportesJson") private String soportesJson;
    // PC (puntos de control): toma reclamada (nulos en un reclamo de la sesion) y resultado de la aprobacion (AN o EX)
    @Column(name = "idToma") private UUID idToma;
    @Column(name = "numeroToma") private Integer numeroToma;
    @JdbcType(LocalDateTimeJdbcType.class)
    @Column(name = "fechaPuntoControlUtc") private LocalDateTime fechaPuntoControlUtc;
    @Column(name = "codigoResultadoAprobacion") private String codigoResultadoAprobacion;

    protected SpSolicitudRevisionEstudianteEntity() { }

    public Integer totalRegistros() { return totalRegistros; }
    public String soportesJson() { return soportesJson; }
    public UUID idToma() { return idToma; }
    public Integer numeroToma() { return numeroToma; }
    public LocalDateTime fechaPuntoControlUtc() { return fechaPuntoControlUtc; }
    public String codigoResultadoAprobacion() { return codigoResultadoAprobacion; }
}
