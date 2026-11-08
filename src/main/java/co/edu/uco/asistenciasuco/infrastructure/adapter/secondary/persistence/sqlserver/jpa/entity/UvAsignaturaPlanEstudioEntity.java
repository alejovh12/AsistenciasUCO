package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/** Modelo de solo lectura sobre {@code dbo.uv_asignatura_plan_estudio}: un vinculo (asignatura, semestre de un plan). CULM-001 B2NM. */
@Entity
@Immutable
@Table(name = "uv_asignatura_plan_estudio", schema = "dbo")
public class UvAsignaturaPlanEstudioEntity {

    @Id
    @Column(name = "idAsignaturaPlanEstudio")
    private UUID idAsignaturaPlanEstudio;

    @Column(name = "idAsignatura")
    private UUID idAsignatura;

    @Column(name = "codigoAsignatura")
    private String codigoAsignatura;

    @Column(name = "nombreAsignatura")
    private String nombreAsignatura;

    @Column(name = "credito")
    private Integer credito;

    @Column(name = "idArea")
    private UUID idArea;

    @Column(name = "nombreArea")
    private String nombreArea;

    @Column(name = "idComponente")
    private UUID idComponente;

    @Column(name = "codigoComponente")
    private String codigoComponente;

    @Column(name = "nombreComponente")
    private String nombreComponente;

    @Column(name = "estaActivaAsignatura")
    private Boolean estaActivaAsignatura;

    @Column(name = "idSemestrePlanEstudio")
    private UUID idSemestrePlanEstudio;

    @Column(name = "numeroSemestre")
    private Integer numeroSemestre;

    @Column(name = "codigoSemestre")
    private String codigoSemestre;

    @Column(name = "idPlanEstudio")
    private UUID idPlanEstudio;

    @Column(name = "inpPlanEstudio")
    private Integer inpPlanEstudio;

    @Column(name = "idPrograma")
    private UUID idPrograma;

    @Column(name = "nombrePrograma")
    private String nombrePrograma;

    @Column(name = "idFacultad")
    private UUID idFacultad;

    @Column(name = "idInstitucion")
    private UUID idInstitucion;

    @Column(name = "codigoCaracter")
    private String codigoCaracter;

    @Column(name = "nombreCaracter")
    private String nombreCaracter;

    @Column(name = "ordenCaracter")
    private Integer ordenCaracter;

    @Column(name = "estaActivoVinculo")
    private Boolean estaActivoVinculo;

    @Column(name = "totalProgramasActivos")
    private Integer totalProgramasActivos;

    protected UvAsignaturaPlanEstudioEntity() {
        // requerido por JPA
    }
}
