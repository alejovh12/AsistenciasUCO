package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDate;
import java.util.UUID;

/** Fila de {@code dbo.usp_consultar_grupos_paginado} (HU064). */
@Entity
public class SpGrupoProgramaPaginadoEntity {
    @Id @Column(name = "id") private UUID id;
    @Column(name = "codigo") private Integer codigo;
    @Column(name = "nombre") private String nombre;
    @Column(name = "idAsignatura") private UUID idAsignatura;
    @Column(name = "nombreAsignatura") private String nombreAsignatura;
    @Column(name = "idDocente") private UUID idDocente;
    @Column(name = "capacidadMaximaPermitida") private Integer capacidadMaximaPermitida;
    @Column(name = "estudiantesActivos") private Integer estudiantesActivos;
    @Column(name = "cuposDisponibles") private Integer cuposDisponibles;
    @Column(name = "grupoEstaHablitado") private Boolean grupoEstaHablitado;
    @Column(name = "fechaInicioPeriodoAcademico") private LocalDate fechaInicioPeriodoAcademico;
    @Column(name = "fechaFinPeriodoAcademico") private LocalDate fechaFinPeriodoAcademico;
    @Column(name = "totalRegistros") private Integer totalRegistros;

    protected SpGrupoProgramaPaginadoEntity() {
    }

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

    public Boolean grupoEstaHablitado() {
        return grupoEstaHablitado;
    }

    public LocalDate fechaInicioPeriodoAcademico() {
        return fechaInicioPeriodoAcademico;
    }

    public LocalDate fechaFinPeriodoAcademico() {
        return fechaFinPeriodoAcademico;
    }

    public Integer totalRegistros() {
        return totalRegistros;
    }
}
