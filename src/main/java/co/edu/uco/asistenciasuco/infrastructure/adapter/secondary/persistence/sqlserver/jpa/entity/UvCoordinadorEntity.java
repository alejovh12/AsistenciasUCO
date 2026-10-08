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
}

