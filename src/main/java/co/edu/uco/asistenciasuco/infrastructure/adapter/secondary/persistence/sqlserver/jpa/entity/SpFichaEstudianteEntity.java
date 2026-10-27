package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

/** Fila de un SP de consulta (no es tabla ni vista). */
@Entity
public class SpFichaEstudianteEntity {
    @Id @Column(name = "idEstudiante") private UUID idEstudiante;
    @Column(name = "tipoIdentificacion") private String tipoIdentificacion;
    @Column(name = "numeroIdentificacion") private Integer numeroIdentificacion;
    @Column(name = "primerNombre") private String primerNombre;
    @Column(name = "segundoNombre") private String segundoNombre;
    @Column(name = "primerApellido") private String primerApellido;
    @Column(name = "segundoApellido") private String segundoApellido;
    @Column(name = "nombreCompleto") private String nombreCompleto;
    @Column(name = "correo") private String correo;
    @Column(name = "estaActivoUsuario") private Boolean estaActivoUsuario;
    @Column(name = "idPrograma") private UUID idPrograma;
    @Column(name = "nombrePrograma") private String nombrePrograma;

    protected SpFichaEstudianteEntity() {
    }

    public UUID idEstudiante() {
        return idEstudiante;
    }

    public String tipoIdentificacion() {
        return tipoIdentificacion;
    }

    public Integer numeroIdentificacion() {
        return numeroIdentificacion;
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

    public String nombreCompleto() {
        return nombreCompleto;
    }

    public String correo() {
        return correo;
    }

    public Boolean estaActivoUsuario() {
        return estaActivoUsuario;
    }

    public UUID idPrograma() {
        return idPrograma;
    }

    public String nombrePrograma() {
        return nombrePrograma;
    }
}
