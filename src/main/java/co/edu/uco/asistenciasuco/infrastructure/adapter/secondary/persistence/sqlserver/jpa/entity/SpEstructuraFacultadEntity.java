package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

/** Fila de {@code dbo.usp_consultar_estructura_facultad} (HU109/HU119); {@code clave} = facultad|programa|plan. */
@Entity
public class SpEstructuraFacultadEntity {
    @Id @Column(name = "clave") private String clave;
    @Column(name = "idFacultad") private UUID idFacultad;
    @Column(name = "nombreFacultad") private String nombreFacultad;
    @Column(name = "nombreCompletoDecano") private String nombreCompletoDecano;
    @Column(name = "estaActivaFacultad") private Boolean estaActivaFacultad;
    @Column(name = "idPrograma") private UUID idPrograma;
    @Column(name = "nombrePrograma") private String nombrePrograma;
    @Column(name = "nombreCoordinador") private String nombreCoordinador;
    @Column(name = "estaActivoPrograma") private Boolean estaActivoPrograma;
    @Column(name = "nombreTipoPrograma") private String nombreTipoPrograma;
    @Column(name = "nombreModalidad") private String nombreModalidad;
    @Column(name = "nombreJornada") private String nombreJornada;
    @Column(name = "idPlanEstudio") private UUID idPlanEstudio;
    @Column(name = "inpPlanEstudio") private Integer inpPlanEstudio;
    @Column(name = "estaActivoPlanEstudio") private Boolean estaActivoPlanEstudio;
    @Column(name = "totalSemestres") private Integer totalSemestres;

    protected SpEstructuraFacultadEntity() {
    }

    public String clave() {
        return clave;
    }

    public UUID idFacultad() {
        return idFacultad;
    }

    public String nombreFacultad() {
        return nombreFacultad;
    }

    public String nombreCompletoDecano() {
        return nombreCompletoDecano;
    }

    public Boolean estaActivaFacultad() {
        return estaActivaFacultad;
    }

    public UUID idPrograma() {
        return idPrograma;
    }

    public String nombrePrograma() {
        return nombrePrograma;
    }

    public String nombreCoordinador() {
        return nombreCoordinador;
    }

    public Boolean estaActivoPrograma() {
        return estaActivoPrograma;
    }

    public String nombreTipoPrograma() {
        return nombreTipoPrograma;
    }

    public String nombreModalidad() {
        return nombreModalidad;
    }

    public String nombreJornada() {
        return nombreJornada;
    }

    public UUID idPlanEstudio() {
        return idPlanEstudio;
    }

    public Integer inpPlanEstudio() {
        return inpPlanEstudio;
    }

    public Boolean estaActivoPlanEstudio() {
        return estaActivoPlanEstudio;
    }

    public Integer totalSemestres() {
        return totalSemestres;
    }
}
