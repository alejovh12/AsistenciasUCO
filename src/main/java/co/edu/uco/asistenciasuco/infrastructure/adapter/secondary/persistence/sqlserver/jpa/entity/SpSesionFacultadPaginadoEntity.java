package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.UUID;

/** Fila de {@code dbo.usp_consultar_sesiones_facultad_paginado} (HU145). */
@Entity
public class SpSesionFacultadPaginadoEntity {
    @Id @Column(name = "id") private UUID id;
    @Column(name = "nombre") private String nombre;
    @Column(name = "codigo") private String codigo;
    @Column(name = "numeroSemana") private Integer numeroSemana;
    @Column(name = "fechaHoraInicio") private LocalDateTime fechaHoraInicio;
    @Column(name = "fechaHoraFin") private LocalDateTime fechaHoraFin;
    @Column(name = "idGrupo") private UUID idGrupo;
    @Column(name = "codigoGrupo") private Integer codigoGrupo;
    @Column(name = "nombreGrupo") private String nombreGrupo;
    @Column(name = "idAsignatura") private UUID idAsignatura;
    @Column(name = "nombreAsignatura") private String nombreAsignatura;
    @Column(name = "idPrograma") private UUID idPrograma;
    @Column(name = "nombrePrograma") private String nombrePrograma;
    @Column(name = "totalRegistros") private Integer totalRegistros;

    protected SpSesionFacultadPaginadoEntity() {
    }

    public UUID id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public String codigo() {
        return codigo;
    }

    public Integer numeroSemana() {
        return numeroSemana;
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

    public String nombreGrupo() {
        return nombreGrupo;
    }

    public UUID idAsignatura() {
        return idAsignatura;
    }

    public String nombreAsignatura() {
        return nombreAsignatura;
    }

    public UUID idPrograma() {
        return idPrograma;
    }

    public String nombrePrograma() {
        return nombrePrograma;
    }

    public Integer totalRegistros() {
        return totalRegistros;
    }
}
