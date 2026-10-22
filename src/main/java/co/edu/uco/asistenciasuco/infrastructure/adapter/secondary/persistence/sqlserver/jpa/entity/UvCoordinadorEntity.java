package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_coordinador} (LB-008 JPA-05).
 * Identidad {@code (id, idPrograma)}: ver {@link UvCoordinadorEntityId}.
 * {@code estaActivoCoordinador} es {@code int} (1/0) en la vista.
 */
@Entity
@Immutable
@IdClass(UvCoordinadorEntityId.class)
@Table(name = "uv_coordinador", schema = "dbo")
public class UvCoordinadorEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "idUsuario")
    private UUID idUsuario;

    @Column(name = "numeroIdentificacion")
    private Integer numeroIdentificacion;

    @Column(name = "nombreCompleto")
    private String nombreCompleto;

    @Id
    @Column(name = "idPrograma")
    private UUID idPrograma;

    @Column(name = "nombrePrograma")
    private String nombrePrograma;

    @Column(name = "idFacultad")
    private UUID idFacultad;

    @Column(name = "estaActivoCoordinador")
    private Integer estaActivoCoordinador;

    @Column(name = "primerNombre")
    private String primerNombre;

    @Column(name = "segundoNombre")
    private String segundoNombre;

    @Column(name = "primerApellido")
    private String primerApellido;

    @Column(name = "segundoApellido")
    private String segundoApellido;

    @Column(name = "correo")
    private String correo;

    @Column(name = "estaActivoRolCoordinador")
    private Boolean estaActivoRolCoordinador;

    protected UvCoordinadorEntity() {
        // requerido por JPA
    }

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

    public UUID idPrograma() {
        return idPrograma;
    }

    public String nombrePrograma() {
        return nombrePrograma;
    }

    public UUID idFacultad() {
        return idFacultad;
    }

    public Integer estaActivoCoordinador() {
        return estaActivoCoordinador;
    }

    public String primerNombre() {
        return primerNombre;
    }

    public String segundoNombre() {
        return segundoNombre;
    }

    public String primerApellido() {
        return primerApellido;
    }

    public String segundoApellido() {
        return segundoApellido;
    }

    public String correo() {
        return correo;
    }

    public Boolean estaActivoRolCoordinador() {
        return estaActivoRolCoordinador;
    }
}

