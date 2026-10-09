package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Entity
@Immutable
@Table(name = "uv_docente", schema = "dbo")
public class UvDocenteEntity {
    @Column(name = "id") private UUID id;
    @Column(name = "idUsuario") private UUID idUsuario;
    @Column(name = "numeroIdentificacion") private Integer numeroIdentificacion;
    @Column(name = "nombreCompleto") private String nombreCompleto;
    @Column(name = "estaActivoUsuario") private Boolean estaActivoUsuario;
    @Column(name = "idInstitucion") private UUID idInstitucion;
    @Column(name = "nombreInstitucion") private String nombreInstitucion;
    @Column(name = "idFacultad") private UUID idFacultad;
    @Column(name = "nombreFacultad") private String nombreFacultad;
    @Column(name = "idPrograma") private UUID idPrograma;
    @Column(name = "nombrePrograma") private String nombrePrograma;
    @Column(name = "idPlanEstudio") private UUID idPlanEstudio;
    @Column(name = "inpPlanEstudio") private Integer inpPlanEstudio;
    @Column(name = "idAsignatura") private UUID idAsignatura;
    @Column(name = "nombreAsignatura") private String nombreAsignatura;
    @Id @Column(name = "idGrupo") private UUID idGrupo;
    @Column(name = "nombreGrupo") private String nombreGrupo;
    @Column(name = "idPerfil") private UUID idPerfil;
    @Column(name = "codigoPerfil") private String codigoPerfil;
    @Column(name = "nombrePerfil") private String nombrePerfil;
    @Column(name = "estaActivoDocente") private Integer estaActivoDocente;
    @Column(name = "estaActivoTextoDocente") private String estaActivoTextoDocente;
    protected UvDocenteEntity() { }

    public UUID id() {
        return id;
    }

    public UUID idUsuario() {
        return idUsuario;
    }

    public Integer numeroIdentificacion() {
        return numeroIdentificacion;
    }

    public String nombreCompleto() {
        return nombreCompleto;
    }

    public Boolean estaActivoUsuario() {
        return estaActivoUsuario;
    }

    public UUID idInstitucion() {
        return idInstitucion;
    }

    public String nombreInstitucion() {
        return nombreInstitucion;
    }

    public UUID idFacultad() {
        return idFacultad;
    }

    public String nombreFacultad() {
        return nombreFacultad;
    }

    public UUID idPrograma() {
        return idPrograma;
    }

    public String nombrePrograma() {
        return nombrePrograma;
    }

    public UUID idPlanEstudio() {
        return idPlanEstudio;
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

    public UUID idGrupo() {
        return idGrupo;
    }

    public String nombreGrupo() {
        return nombreGrupo;
    }

    public UUID idPerfil() {
        return idPerfil;
    }

    public String codigoPerfil() {
        return codigoPerfil;
    }

    public String nombrePerfil() {
        return nombrePerfil;
    }

    public Integer estaActivoDocente() {
        return estaActivoDocente;
    }

    public String estaActivoTextoDocente() {
        return estaActivoTextoDocente;
    }
}

