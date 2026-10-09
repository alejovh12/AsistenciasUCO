package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_horario_docente} (LB-008 JPA-05).
 * {@code horaInicio}/{@code horaFin} son {@code varchar(5)} ("HH:mm") y son hora academica LOCAL:
 * NO aplicar el mapper UTC de Sesion.
 */
@Entity
@Immutable
@Table(name = "uv_horario_docente", schema = "dbo")
public class UvHorarioDocenteEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "idDocente")
    private UUID idDocente;

    @Column(name = "idGrupo")
    private UUID idGrupo;

    @Column(name = "codigoMateria")
    private String codigoMateria;

    @Column(name = "nombreMateria")
    private String nombreMateria;

    @Column(name = "seccion")
    private String seccion;

    @Column(name = "dia")
    private String dia;

    @Column(name = "horaInicio")
    private String horaInicio;

    @Column(name = "horaFin")
    private String horaFin;

    @Column(name = "totalEstudiantes")
    private Integer totalEstudiantes;

    protected UvHorarioDocenteEntity() {
        // requerido por JPA
    }

    public UUID id() {
        return id;
    }

    public UUID idDocente() {
        return idDocente;
    }

    public UUID idGrupo() {
        return idGrupo;
    }

    public String codigoMateria() {
        return codigoMateria;
    }

    public String nombreMateria() {
        return nombreMateria;
    }

    public String seccion() {
        return seccion;
    }

    public String dia() {
        return dia;
    }

    public String horaInicio() {
        return horaInicio;
    }

    public String horaFin() {
        return horaFin;
    }

    public Integer totalEstudiantes() {
        return totalEstudiantes;
    }
}

