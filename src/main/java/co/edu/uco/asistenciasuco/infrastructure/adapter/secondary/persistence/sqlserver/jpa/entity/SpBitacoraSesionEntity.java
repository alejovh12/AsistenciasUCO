package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;

/** Fila de un SP de consulta (no es tabla ni vista). */
@Entity
public class SpBitacoraSesionEntity {
    @Id @Column(name = "idSesion") private UUID idSesion;
    @Column(name = "numeroSesion") private Integer numeroSesion;
    @Column(name = "nombreSesion") private String nombreSesion;
    @JdbcType(LocalDateTimeJdbcType.class)
    @Column(name = "fechaHoraInicio") private LocalDateTime fechaHoraInicio;
    @JdbcType(LocalDateTimeJdbcType.class)
    @Column(name = "fechaHoraFin") private LocalDateTime fechaHoraFin;
    @Column(name = "idGrupo") private UUID idGrupo;
    @Column(name = "codigoGrupo") private Integer codigoGrupo;
    @Column(name = "idAsignatura") private UUID idAsignatura;
    @Column(name = "nombreAsignatura") private String nombreAsignatura;
    @Column(name = "asistio") private Boolean asistio;
    @Column(name = "codigoRazonCausa") private String codigoRazonCausa;
    @Column(name = "nombreRazonCausa") private String nombreRazonCausa;
    @Column(name = "totalRegistros") private Integer totalRegistros;

    protected SpBitacoraSesionEntity() {
    }

    public UUID idSesion() {
        return idSesion;
    }

    public Integer numeroSesion() {
        return numeroSesion;
    }

    public String nombreSesion() {
        return nombreSesion;
    }

    public LocalDateTime fechaHoraInicio() {
        return fechaHoraInicio;
    }

    public LocalDateTime fechaHoraFin() {
        return fechaHoraFin;
    }

    public UUID idGrupo() {
        return idGrupo;
    }

    public Integer codigoGrupo() {
        return codigoGrupo;
    }

    public UUID idAsignatura() {
        return idAsignatura;
    }

    public String nombreAsignatura() {
        return nombreAsignatura;
    }

    public Boolean asistio() {
        return asistio;
    }

    public String codigoRazonCausa() {
        return codigoRazonCausa;
    }

    public String nombreRazonCausa() {
        return nombreRazonCausa;
    }

    public Integer totalRegistros() {
        return totalRegistros;
    }
}
