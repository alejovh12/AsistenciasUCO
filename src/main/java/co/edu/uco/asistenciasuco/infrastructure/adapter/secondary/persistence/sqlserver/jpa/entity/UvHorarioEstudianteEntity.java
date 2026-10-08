package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_horario_estudiante} (LB-008 JPA-05).
 * Identidad {@code (id, idEstudiante)}: ver {@link UvHorarioEstudianteEntityId}.
 * {@code horaInicio}/{@code horaFin} son {@code varchar(5)} y hora academica LOCAL (no UTC).
 */
@Entity
@Immutable
@IdClass(UvHorarioEstudianteEntityId.class)
@Table(name = "uv_horario_estudiante", schema = "dbo")
public class UvHorarioEstudianteEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Id
    @Column(name = "idEstudiante")
    private UUID idEstudiante;

    @Column(name = "idGrupo")
    private UUID idGrupo;

    @Column(name = "codigoMateria")
    private String codigoMateria;

    @Column(name = "nombreMateria")
    private String nombreMateria;

    @Column(name = "grupo")
    private String grupo;

    @Column(name = "dia")
    private String dia;

    @Column(name = "horaInicio")
    private String horaInicio;

    @Column(name = "horaFin")
    private String horaFin;

    @Column(name = "docente")
    private String docente;

    protected UvHorarioEstudianteEntity() {
        // requerido por JPA
    }

    public UUID id() {
        return id;
    }

    public UUID idEstudiante() {
        return idEstudiante;
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

    public String grupo() {
        return grupo;
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

    public String docente() {
        return docente;
    }
}

