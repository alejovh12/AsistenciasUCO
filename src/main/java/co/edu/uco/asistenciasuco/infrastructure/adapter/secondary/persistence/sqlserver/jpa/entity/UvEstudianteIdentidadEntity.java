package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Entity
@Immutable
@Table(name = "uv_estudiante_identidad", schema = "dbo")
public class UvEstudianteIdentidadEntity {
    @Id @Column(name = "id") private UUID id;
    @Column(name = "idUsuario") private UUID idUsuario;
    @Column(name = "numeroIdentificacion") private Integer numeroIdentificacion;
    @Column(name = "nombreCompleto") private String nombreCompleto;
    @Column(name = "estaActivoUsuario") private Boolean estaActivoUsuario;
    protected UvEstudianteIdentidadEntity() { }
}
