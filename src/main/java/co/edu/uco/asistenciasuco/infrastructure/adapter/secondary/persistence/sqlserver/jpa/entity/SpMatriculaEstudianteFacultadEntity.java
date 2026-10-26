package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

/** Fila de {@code dbo.usp_consultar_estudiante_facultad_detalle} (HU105): una matricula por grupo. */
@Entity
public class SpMatriculaEstudianteFacultadEntity {
    @Id @Column(name = "idGrupo") private UUID idGrupo;
    @Column(name = "idEstudiante") private UUID idEstudiante;
    @Column(name = "numeroIdentificacion") private Integer numeroIdentificacion;
    @Column(name = "nombreCompleto") private String nombreCompleto;
    @Column(name = "idPrograma") private UUID idPrograma;
    @Column(name = "nombrePrograma") private String nombrePrograma;
    @Column(name = "inpPlanEstudio") private Integer inpPlanEstudio;
    @Column(name = "idAsignatura") private UUID idAsignatura;
    @Column(name = "nombreAsignatura") private String nombreAsignatura;
    @Column(name = "nombreGrupo") private String nombreGrupo;
    @Column(name = "estaActivoEstudiante") private Boolean estaActivoEstudiante;

    protected SpMatriculaEstudianteFacultadEntity() {
    }

    public UUID idGrupo() {
        return idGrupo;
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

    public UUID idPrograma() {
        return idPrograma;
    }

    public String nombrePrograma() {
        return nombrePrograma;
    }

    public Integer inpPlanEstudio() {
        return inpPlanEstudio;
    }

    public UUID idAsignatura() {
        return idAsignatura;
    }

    public String nombreAsignatura() {
        return nombreAsignatura;
    }

    public String nombreGrupo() {
        return nombreGrupo;
    }

    public Boolean estaActivoEstudiante() {
        return estaActivoEstudiante;
    }
}
