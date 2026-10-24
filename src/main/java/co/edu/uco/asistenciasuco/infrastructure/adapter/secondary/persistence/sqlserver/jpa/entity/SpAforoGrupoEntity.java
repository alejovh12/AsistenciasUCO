package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.util.UUID;

/** Fila de un SP de consulta (no es tabla ni vista). */
@Entity
public class SpAforoGrupoEntity {
    @Id @Column(name = "idGrupo") private UUID idGrupo;
    @Column(name = "codigoGrupo") private Integer codigoGrupo;
    @Column(name = "idAsignatura") private UUID idAsignatura;
    @Column(name = "nombreAsignatura") private String nombreAsignatura;
    @Column(name = "idPrograma") private UUID idPrograma;
    @Column(name = "nombrePrograma") private String nombrePrograma;
    @Column(name = "cupoMaximo") private Integer cupoMaximo;
    @Column(name = "matriculadosActivos") private Integer matriculadosActivos;
    @Column(name = "cuposDisponibles") private Integer cuposDisponibles;
    @Column(name = "tasaAforo") private BigDecimal tasaAforo;
    @Column(name = "grupoEstaHabilitado") private Boolean grupoEstaHabilitado;
    @Column(name = "totalRegistros") private Integer totalRegistros;

    protected SpAforoGrupoEntity() {
    }

    public UUID idGrupo() {
        return idGrupo;
    }

    public Integer codigoGrupo() {
        return codigoGrupo;
    }

    public UUID idAsignatura() {
        return idAsignatura;
    }

    public String nombreAsignatura() {
        return nombreAsignatura;
    }

    public UUID idPrograma() {
        return idPrograma;
    }

    public String nombrePrograma() {
        return nombrePrograma;
    }

    public Integer cupoMaximo() {
        return cupoMaximo;
    }

    public Integer matriculadosActivos() {
        return matriculadosActivos;
    }

    public Integer cuposDisponibles() {
        return cuposDisponibles;
    }

    public BigDecimal tasaAforo() {
        return tasaAforo;
    }

    public Boolean grupoEstaHabilitado() {
        return grupoEstaHabilitado;
    }

    public Integer totalRegistros() {
        return totalRegistros;
    }
}
