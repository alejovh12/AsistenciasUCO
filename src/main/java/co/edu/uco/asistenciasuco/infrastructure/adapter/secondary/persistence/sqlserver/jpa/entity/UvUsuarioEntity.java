package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Entity
@Immutable
@Table(name = "uv_usuario", schema = "dbo")
public class UvUsuarioEntity {
    @Id @Column(name = "id") private UUID id;
    @Column(name = "idTipoIdentificacion") private UUID idTipoIdentificacion;
    @Column(name = "numeroIdentificacion") private Integer numeroIdentificacion;
    @Column(name = "primerApellido") private String primerApellido;
    @Column(name = "segundoApellido") private String segundoApellido;
    @Column(name = "primerNombre") private String primerNombre;
    @Column(name = "segundoNombre") private String segundoNombre;
    @Column(name = "nombreCompleto") private String nombreCompleto;
    @Column(name = "correo") private String correo;
    @Column(name = "estaActivoUsuario") private Boolean estaActivoUsuario;
    protected UvUsuarioEntity() { }

    public UUID id() {
        return id;
    }

    public UUID idTipoIdentificacion() {
        return idTipoIdentificacion;
    }

    public Integer numeroIdentificacion() {
        return numeroIdentificacion;
    }

    public String primerApellido() {
        return primerApellido;
    }

    public String segundoApellido() {
        return segundoApellido;
    }

    public String primerNombre() {
        return primerNombre;
    }

    public String segundoNombre() {
        return segundoNombre;
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
}

