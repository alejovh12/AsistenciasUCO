package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.Date;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "uv_sesion", schema = "dbo")
public class UvSesionEntity {
    @Id @Column(name = "id") private UUID id;
    @Column(name = "idGrupo") private UUID idGrupo;
    @Column(name = "nombre") private String nombre;
    @Column(name = "numero") private Integer numero;
    @Column(name = "codigo") private String codigo;
    @Column(name = "numeroSemana") private Integer numeroSemana;
    @Column(name = "codigoGrupo") private Integer codigoGrupo;
    @Column(name = "nombreGrupo") private String nombreGrupo;
    @Column(name = "fechaHoraInicio") private Date fechaHoraInicio;
    @Column(name = "fechaHoraFin") private Date fechaHoraFin;
    protected UvSesionEntity() { }

    public UUID id() {
        return id;
    }

    public UUID idGrupo() {
        return idGrupo;
    }

    public String nombre() {
        return nombre;
    }

    public Integer numero() {
        return numero;
    }

    public String codigo() {
        return codigo;
    }

    public Integer numeroSemana() {
        return numeroSemana;
    }

    public Integer codigoGrupo() {
        return codigoGrupo;
    }

    public String nombreGrupo() {
        return nombreGrupo;
    }

    public Date fechaHoraInicio() {
        return fechaHoraInicio;
    }

    public Date fechaHoraFin() {
        return fechaHoraFin;
    }
}

