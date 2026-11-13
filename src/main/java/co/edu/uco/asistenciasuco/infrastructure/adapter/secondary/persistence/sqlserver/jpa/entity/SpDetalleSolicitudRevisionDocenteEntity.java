package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fila de resultado de {@code dbo.usp_consultar_detalle_solicitud_revision_docente} (HU036). No es una tabla ni
 * una vista. Los DATETIME2(7) se leen con {@link LocalDateTimeJdbcType} (sin zona de la JVM).
 */
@Entity
public class SpDetalleSolicitudRevisionDocenteEntity extends SpSolicitudRevisionDocenteBaseEntity {
    @Column(name = "asistio") private Boolean asistio;
    @Column(name = "codigoRazonCausa") private String codigoRazonCausa;
    @Column(name = "nombreRazonCausa") private String nombreRazonCausa;
    // CULM-001 D.2: arreglo JSON [{idArchivo, nombreOriginal, tipoContenido, tamanioBytes}] ordenado por adjuncion
    @Column(name = "soportesJson") private String soportesJson;
    // PC (puntos de control): toma reclamada (nulos en un reclamo de la sesion), resultado de la aprobacion (AN o EX), marca actual
    // del estudiante en la toma y minutos de la sesion
    @Column(name = "idToma") private UUID idToma;
    @Column(name = "numeroToma") private Integer numeroToma;
    @JdbcType(LocalDateTimeJdbcType.class)
    @Column(name = "fechaPuntoControlUtc") private LocalDateTime fechaPuntoControlUtc;
    @Column(name = "codigoResultadoAprobacion") private String codigoResultadoAprobacion;
    @Column(name = "codigoRazonCausaToma") private String codigoRazonCausaToma;
    @Column(name = "minutosSesion") private Integer minutosSesion;
    @Column(name = "minutosAsistidos") private Integer minutosAsistidos;

    protected SpDetalleSolicitudRevisionDocenteEntity() { }

    public Boolean asistio() { return asistio; }
    public String codigoRazonCausa() { return codigoRazonCausa; }
    public String nombreRazonCausa() { return nombreRazonCausa; }
    public String soportesJson() { return soportesJson; }
    public UUID idToma() { return idToma; }
    public Integer numeroToma() { return numeroToma; }
    public LocalDateTime fechaPuntoControlUtc() { return fechaPuntoControlUtc; }
    public String codigoResultadoAprobacion() { return codigoResultadoAprobacion; }
    public String codigoRazonCausaToma() { return codigoRazonCausaToma; }
    public Integer minutosSesion() { return minutosSesion; }
    public Integer minutosAsistidos() { return minutosAsistidos; }
}
