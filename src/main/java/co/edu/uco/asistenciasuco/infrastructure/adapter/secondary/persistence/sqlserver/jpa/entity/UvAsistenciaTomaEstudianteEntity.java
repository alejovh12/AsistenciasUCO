package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PC (puntos de control): proyeccion de SOLO LECTURA de {@code dbo.uv_asistencia_toma_estudiante} (una fila por cabecera de
 * asistencia y toma de la sesion). Estado de la toma, reclamabilidad y solicitud los calcula la base de datos.
 */
@Entity
@Immutable
@IdClass(UvAsistenciaTomaEstudianteEntity.Clave.class)
@Table(name = "uv_asistencia_toma_estudiante", schema = "dbo")
public class UvAsistenciaTomaEstudianteEntity {

    /** Clave compuesta de la vista: cabecera de asistencia y toma. */
    public record Clave(UUID idAsistencia, UUID idToma) implements Serializable {
    }

    @Id @Column(name = "idAsistencia") private UUID idAsistencia;
    @Id @Column(name = "idToma") private UUID idToma;
    @Column(name = "idEstudianteGrupo") private UUID idEstudianteGrupo;
    @Column(name = "idSesion") private UUID idSesion;
    @Column(name = "numeroToma") private Integer numeroToma;
    @Column(name = "ordenPuntoControl") private Integer ordenPuntoControl;
    @JdbcType(LocalDateTimeJdbcType.class)
    @Column(name = "fechaPuntoControlUtc") private LocalDateTime fechaPuntoControlUtc;
    @Column(name = "codigoRazonCausa") private String codigoRazonCausa;
    @Column(name = "idSolicitudRevision") private UUID idSolicitudRevision;
    @Column(name = "codigoEstadoSolicitud") private String codigoEstadoSolicitud;
    @Column(name = "codigoResultadoAprobacion") private String codigoResultadoAprobacion;
    @Column(name = "esReclamable") private Boolean esReclamable;
    @Column(name = "idEstudiante") private UUID idEstudiante;
    @Column(name = "codigoEstadoTomaEstudiante") private String codigoEstadoTomaEstudiante;

    protected UvAsistenciaTomaEstudianteEntity() {
        // requerido por JPA
    }
}
