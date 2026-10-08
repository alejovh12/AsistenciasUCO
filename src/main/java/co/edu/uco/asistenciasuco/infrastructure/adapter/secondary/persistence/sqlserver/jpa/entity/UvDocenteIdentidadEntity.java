package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Entity
@Immutable
@Table(name = "uv_docente_identidad", schema = "dbo")
public class UvDocenteIdentidadEntity {
    @Id @Column(name = "id") private UUID id;
    @Column(name = "idUsuario") private UUID idUsuario;
    @Column(name = "numeroIdentificacion") private Integer numeroIdentificacion;
    @Column(name = "nombreCompleto") private String nombreCompleto;
    @Column(name = "estaActivoUsuario") private Boolean estaActivoUsuario;
    protected UvDocenteIdentidadEntity() { }

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

    public Boolean estaActivoUsuario() {
        return estaActivoUsuario;
    }
}

