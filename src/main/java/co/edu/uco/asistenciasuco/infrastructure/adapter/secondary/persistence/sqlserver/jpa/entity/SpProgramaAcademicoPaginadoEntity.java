package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
public class SpProgramaAcademicoPaginadoEntity {
    @Id @Column(name = "id") private UUID id;
    @Column(name = "nombrePrograma") private String nombrePrograma;
    @Column(name = "idTipoPrograma") private UUID idTipoPrograma;
    @Column(name = "nombreTipoPrograma") private String nombreTipoPrograma;
    @Column(name = "idModalidad") private UUID idModalidad;
    @Column(name = "nombreModalidad") private String nombreModalidad;
    @Column(name = "idJornada") private UUID idJornada;
    @Column(name = "nombreJornada") private String nombreJornada;
    @Column(name = "idCoordinador") private UUID idCoordinador;
    @Column(name = "nombreCoordinador") private String nombreCoordinador;
    @Column(name = "idFacultad") private UUID idFacultad;
    @Column(name = "nombreFacultad") private String nombreFacultad;
    @Column(name = "estaActivoPrograma") private Boolean estaActivoPrograma;
    @Column(name = "totalRegistros") private Integer totalRegistros;

    protected SpProgramaAcademicoPaginadoEntity() {
    }

    public UUID id() {
        return id;
    }

    public String nombrePrograma() {
        return nombrePrograma;
    }

    public UUID idTipoPrograma() {
        return idTipoPrograma;
    }

    public String nombreTipoPrograma() {
        return nombreTipoPrograma;
    }

    public UUID idModalidad() {
        return idModalidad;
    }

    public String nombreModalidad() {
        return nombreModalidad;
    }

    public UUID idJornada() {
        return idJornada;
    }

    public String nombreJornada() {
        return nombreJornada;
    }

    public UUID idCoordinador() {
        return idCoordinador;
    }

    public String nombreCoordinador() {
        return nombreCoordinador;
    }

    public UUID idFacultad() {
        return idFacultad;
    }

    public String nombreFacultad() {
        return nombreFacultad;
    }

    public Boolean estaActivoPrograma() {
        return estaActivoPrograma;
    }

    public Integer totalRegistros() {
        return totalRegistros;
    }
}
