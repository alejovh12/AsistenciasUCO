package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.UUID;

/** Fila de {@code dbo.usp_consultar_asistencias_sesion_paginado} (HU061). */
@Entity
public class SpAsistenciaSesionPaginadoEntity {
    @Id @Column(name = "idDetalleAsistencia") private UUID idDetalleAsistencia;
    @Column(name = "idSesion") private UUID idSesion;
    @Column(name = "nombreSesion") private String nombreSesion;
    @Column(name = "idEstudiante") private UUID idEstudiante;
    @Column(name = "numeroIdentificacion") private Integer numeroIdentificacion;
    @Column(name = "nombreCompleto") private String nombreCompleto;
    @Column(name = "asistio") private Boolean asistio;
    @Column(name = "codigoRazonCausa") private String codigoRazonCausa;
    @Column(name = "nombreRazonCausa") private String nombreRazonCausa;
    @Column(name = "fechaHoraInicio") private LocalDateTime fechaHoraInicio;
    @Column(name = "fechaHoraFin") private LocalDateTime fechaHoraFin;
    @Column(name = "totalRegistros") private Integer totalRegistros;

    protected SpAsistenciaSesionPaginadoEntity() {
    }

    public UUID idDetalleAsistencia() {
        return idDetalleAsistencia;
    }

    public UUID idSesion() {
        return idSesion;
    }

    public String nombreSesion() {
        return nombreSesion;
    }

    public UUID idEstudiante() {
        return idEstudiante;
    }

    public Integer numeroIdentificacion() {
        return numeroIdentificacion;
    }

    public String nombreCompleto() {
        return nombreCompleto;
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

    public LocalDateTime fechaHoraInicio() {
        return fechaHoraInicio;
    }

    public LocalDateTime fechaHoraFin() {
        return fechaHoraFin;
    }

    public Integer totalRegistros() {
        return totalRegistros;
    }
}
