package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "uv_grupo", schema = "dbo")
public class UvGrupoEntity {
    @Id @Column(name = "id") private UUID id;
    @Column(name = "codigo") private Integer codigo;
    @Column(name = "nombre") private String nombre;
    @Column(name = "idAsignatura") private UUID idAsignatura;
    @Column(name = "nombreAsignatura") private String nombreAsignatura;
    @Column(name = "idDocente") private UUID idDocente;
    @Column(name = "capacidadMaximaPermitida") private Integer capacidadMaximaPermitida;
    @Column(name = "estudiantesActivos") private Integer estudiantesActivos;
    @Column(name = "cuposDisponibles") private Integer cuposDisponibles;
    @Column(name = "grupoEstaHablitado") private Integer grupoEstaHablitado;
    @Column(name = "fechaInicioPeriodoAcademico") private LocalDate fechaInicioPeriodoAcademico;
    @Column(name = "fechaFinPeriodoAcademico") private LocalDate fechaFinPeriodoAcademico;
    protected UvGrupoEntity() { }

    public UUID id() {
        return id;
    }

    public Integer codigo() {
        return codigo;
    }

    public String nombre() {
        return nombre;
    }

    public UUID idAsignatura() {
        return idAsignatura;
    }

    public String nombreAsignatura() {
        return nombreAsignatura;
    }

    public UUID idDocente() {
        return idDocente;
    }

    public Integer capacidadMaximaPermitida() {
        return capacidadMaximaPermitida;
    }

    public Integer estudiantesActivos() {
        return estudiantesActivos;
    }

    public Integer cuposDisponibles() {
        return cuposDisponibles;
    }

    public Integer grupoEstaHablitado() {
        return grupoEstaHablitado;
    }

    public LocalDate fechaInicioPeriodoAcademico() {
        return fechaInicioPeriodoAcademico;
    }

    public LocalDate fechaFinPeriodoAcademico() {
        return fechaFinPeriodoAcademico;
    }
}

