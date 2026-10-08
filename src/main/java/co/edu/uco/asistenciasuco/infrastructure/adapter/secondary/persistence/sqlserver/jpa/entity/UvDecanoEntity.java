package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_decano} (LB-008 JPA-05).
 * Identidad {@code (id, idFacultad)}: ver {@link UvDecanoEntityId}.
 * {@code estaActivoDecano} es {@code int} (1/0) en la vista.
 */
@Entity
@Immutable
@IdClass(UvDecanoEntityId.class)
@Table(name = "uv_decano", schema = "dbo")
public class UvDecanoEntity {

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
    @Column(name = "idFacultad")
    private UUID idFacultad;

    @Column(name = "nombreFacultad")
    private String nombreFacultad;

    @Column(name = "estaActivoDecano")
    private Integer estaActivoDecano;

    protected UvDecanoEntity() {
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

    public UUID idFacultad() {
        return idFacultad;
    }

    public String nombreFacultad() {
        return nombreFacultad;
    }

    public Integer estaActivoDecano() {
        return estaActivoDecano;
    }
}

