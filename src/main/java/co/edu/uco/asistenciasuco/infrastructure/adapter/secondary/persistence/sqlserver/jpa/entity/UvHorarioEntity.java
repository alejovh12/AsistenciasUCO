package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_horario} (F1): franjas del horario semanal de
 * cada grupo. {@code horaInicio}/{@code horaFin} son hora academica LOCAL: NO aplicar el mapper UTC de Sesion.
 */
@Entity
@Immutable
@Table(name = "uv_horario", schema = "dbo")
public class UvHorarioEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "idGrupo")
    private UUID idGrupo;

    @Column(name = "codigoDia")
    private String codigoDia;

    @Column(name = "nombreDia")
    private String nombreDia;

    @Column(name = "horaInicio")
    private LocalTime horaInicio;

    @Column(name = "horaFin")
    private LocalTime horaFin;

    protected UvHorarioEntity() {
        // requerido por JPA
    }

    public UUID id() {
        return id;
    }

    public UUID idGrupo() {
        return idGrupo;
    }

    public String codigoDia() {
        return codigoDia;
    }

    public String nombreDia() {
        return nombreDia;
    }

    public LocalTime horaInicio() {
        return horaInicio;
    }

    public LocalTime horaFin() {
        return horaFin;
    }
}
