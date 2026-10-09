package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_plan_estudio} (LB-008 JPA-05).
 * {@code estaActivoPlanEstudio} es {@code int} en la vista (1/0), no {@code bit}.
 */
@Entity
@Immutable
@Table(name = "uv_plan_estudio", schema = "dbo")
public class UvPlanEstudioEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "idPrograma")
    private UUID idPrograma;

    @Column(name = "nombrePrograma")
    private String nombrePrograma;

    @Column(name = "inp")
    private Integer inp;

    @Column(name = "estaActivoPlanEstudio")
    private Integer estaActivoPlanEstudio;

    @Column(name = "estaActivoTextoPlanEstudio")
    private String estaActivoTextoPlanEstudio;

    @Column(name = "justificacionEstado")
    private String justificacionEstado;

    protected UvPlanEstudioEntity() {
        // requerido por JPA
    }

    public UUID id() {
        return id;
    }

    public UUID idPrograma() {
        return idPrograma;
    }

    public String nombrePrograma() {
        return nombrePrograma;
    }

    public Integer inp() {
        return inp;
    }

    public Integer estaActivoPlanEstudio() {
        return estaActivoPlanEstudio;
    }

    public String estaActivoTextoPlanEstudio() {
        return estaActivoTextoPlanEstudio;
    }

    public String justificacionEstado() {
        return justificacionEstado;
    }
}

