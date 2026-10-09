package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_semestre_plan_estudio} (LB-008 JPA-05).
 * Se usa como tabla de unión para filtrar asignaturas por programa o plan de estudio.
 */
@Entity
@Immutable
@Table(name = "uv_semestre_plan_estudio", schema = "dbo")
public class UvSemestrePlanEstudioEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "idPlanEstudio")
    private UUID idPlanEstudio;

    @Column(name = "inp")
    private Integer inp;

    @Column(name = "idPrograma")
    private UUID idPrograma;

    @Column(name = "nombrePrograma")
    private String nombrePrograma;

    @Column(name = "idSemestre")
    private UUID idSemestre;

    @Column(name = "numeroSemestre")
    private Integer numeroSemestre;

    protected UvSemestrePlanEstudioEntity() {
        // requerido por JPA
    }
}
