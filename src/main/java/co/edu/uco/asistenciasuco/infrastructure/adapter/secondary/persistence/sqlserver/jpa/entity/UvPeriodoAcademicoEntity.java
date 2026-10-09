package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_periodo_academico} (LB-008 JPA-05).
 */
@Entity
@Immutable
@Table(name = "uv_periodo_academico", schema = "dbo")
public class UvPeriodoAcademicoEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "idInstitucion")
    private UUID idInstitucion;

    @Column(name = "nombreInstitucion")
    private String nombreInstitucion;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "codigo")
    private Integer codigo;

    @Column(name = "fechaInicio")
    private LocalDate fechaInicio;

    @Column(name = "fechaFin")
    private LocalDate fechaFin;

    @Column(name = "anio")
    private Integer anio;

    protected UvPeriodoAcademicoEntity() {
        // requerido por JPA
    }

    public UUID id() {
        return id;
    }

    public UUID idInstitucion() {
        return idInstitucion;
    }

    public String nombreInstitucion() {
        return nombreInstitucion;
    }

    public String nombre() {
        return nombre;
    }

    public Integer codigo() {
        return codigo;
    }

    public LocalDate fechaInicio() {
        return fechaInicio;
    }

    public LocalDate fechaFin() {
        return fechaFin;
    }

    public Integer anio() {
        return anio;
    }
}

